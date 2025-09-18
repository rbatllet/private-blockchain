#!/usr/bin/env zsh
# Concurrent SQLite write stress test (zsh)
# Usage: ./scripts/sqlite_concurrent_test.zsh [--db path] [--parallel N] [--inserts M] [--table name] [--use-copy]
# Defaults: db=./blockchain.db, parallel=50, inserts=200, table=concurrency_test

set -euo pipefail

DB="./blockchain.db"
PARALLEL=50
INSERTS=200
TABLE="concurrency_test"
USE_COPY=0
ERROR_LOG="/tmp/sqlite_concurrent_errors_$$.log"

print_help() {
  cat <<EOF
Usage: $0 [options]

Options:
  --db PATH         Path to sqlite database file (default: ./blockchain.db)
  --parallel N      Number of concurrent writers/processes (default: 50)
  --inserts M       Number of inserts per writer (default: 200)
  --table NAME      Table name to use/create (default: concurrency_test)
  --use-copy        Work on a temp copy of the DB instead of the original (safer)
  -h, --help        Show this help

Example:
  $0 --db ./blockchain.db --parallel 20 --inserts 100 --use-copy

Note: This script uses the 'sqlite3' CLI. Ensure it is installed before running.
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case $1 in
    --db)
      DB="$2"; shift 2;;
    --parallel)
      PARALLEL=$2; shift 2;;
    --inserts)
      INSERTS=$2; shift 2;;
    --table)
      TABLE=$2; shift 2;;
    --use-copy)
      USE_COPY=1; shift 1;;
    -h|--help)
      print_help; exit 0;;
    *)
      echo "Unknown option: $1" >&2; print_help; exit 2;;
  esac
done

if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "ERROR: sqlite3 CLI not found in PATH." >&2
  exit 3
fi

if [[ ! -f $DB ]]; then
  echo "WARNING: DB file '$DB' does not exist. The script will create it." >&2
fi

TMP_DB="${DB%.*}_test_copy_$$.db"
if [[ $USE_COPY -eq 1 ]]; then
  echo "Creating temp DB copy: $TMP_DB"
  cp "$DB" "$TMP_DB" 2>/dev/null || true
  DB="$TMP_DB"
fi

echo "Using DB: $DB"
echo "Parallel writers: $PARALLEL, inserts/writer: $INSERTS, table: $TABLE"

# Prepare table
sqlite3 "$DB" <<SQL
CREATE TABLE IF NOT EXISTS ${TABLE} (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  writer INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  payload TEXT
);
SQL

# Clear error log
: > "$ERROR_LOG"

start_time=$(date +%s)

echo "Starting concurrent workers..."

for (( w=1; w<=PARALLEL; w++ )); do
  (
    for (( i=1; i<=INSERTS; i++ )); do
      if ! sqlite3 "$DB" "BEGIN TRANSACTION; INSERT INTO ${TABLE}(writer,payload) VALUES(${w}, 'payload ${i}'); COMMIT;" 2>>"$ERROR_LOG"; then
        echo "
$(date +%Y-%m-%dT%H:%M:%S) ERROR writer=${w} insert=${i} (rc=$?)" >>"$ERROR_LOG"
      fi
    done
  ) &
done

wait

end_time=$(date +%s)
duration=$((end_time - start_time))

row_count=$(sqlite3 "$DB" "SELECT count(*) FROM ${TABLE};" 2>/dev/null || echo "0")
error_count=0
if [[ -f "$ERROR_LOG" ]]; then
  error_count=$(wc -l < "$ERROR_LOG" || echo 0)
fi

echo "Done. Duration: ${duration}s"
echo "Inserted rows (table ${TABLE}): ${row_count}"
echo "Errors logged: ${error_count} (see $ERROR_LOG)"

if [[ $USE_COPY -eq 1 ]]; then
  echo "Note: temporary DB copy used at: $TMP_DB (you can remove it)"
fi

exit 0
