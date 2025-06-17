# Error Handling Standard for Scripts

This document describes the error handling standard adopted for all shell scripts in the Private Blockchain project.

## General Principles

1. **Consistency**: All scripts must follow the same error handling pattern.
2. **Clarity**: Error messages must be clear, concise, and descriptive.
3. **Visibility**: Errors should be visually highlighted for easy identification.
4. **Actionability**: Messages should provide sufficient information to resolve the issue.

## Error Handling Functions

All error handling functions are defined in the `scripts/shared-functions.sh` file.

### Main Functions

#### `error_exit`

This is the main function for handling fatal errors that require script termination.

```bash
error_exit() {
    print_error "ERROR: $1"
    print_error "Aborting operation."
    exit 1
}
```

**Usage**:
```bash
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Please run this script from the project root directory."
fi
```

#### `print_error`

Function to print error messages without terminating execution.

```bash
print_error() {
    echo -e "${RED}❌ $1${NC}"
}
```

**Usage**:
```bash
if [ "$value" -lt 0 ]; then
    print_error "Negative value detected: $value"
    # Continue execution or handle the error in another way
fi
```

### Other Output Functions

- `print_warning`: For warnings that are not fatal errors.
- `print_info`: For informational messages.
- `print_success`: To indicate successful operations.
- `print_step`: To indicate steps in a process.

## Common Patterns

### Project Directory Verification

```bash
# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Please run this script from the project root directory."
fi
```

### Compilation Verification

```bash
print_step "Compiling project..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    error_exit "Compilation failed. Please check the errors above."
fi
```

### Dependencies Verification

```bash
if ! command -v java &> /dev/null; then
    error_exit "Java not found. Please install Java and try again."
fi
```

## Best Practices

1. **Always use `error_exit` for fatal errors** instead of `print_error` followed by `exit 1`.
2. **Provide descriptive error messages** that help identify and resolve the problem.
3. **Include context information** when relevant (e.g., variable values, file paths).
4. **Check error conditions early** in the script to fail fast.
5. **Use appropriate output functions** according to the severity of the message.

## Examples

### Example 1: File Verification

```bash
if [ ! -f "$config_file" ]; then
    error_exit "Configuration file not found: $config_file"
fi
```

### Example 2: Command Execution

```bash
print_step "Running database migration..."
if ! ./migrate_db.sh; then
    error_exit "Database migration failed"
fi
print_success "Database migration completed successfully"
```

### Example 3: Test Result Verification

```bash
if [ "$failed_tests" -gt 0 ]; then
    error_exit "$failed_tests tests failed. Please check the test output for details."
fi
```

## Migration from Previous Standards

This error handling standard was implemented as part of the migration from Bash to ZSH and the standardization of project scripts. All scripts have been updated to follow this standard.

For more information about the migration from Bash to ZSH, see [BASH_TO_ZSH_MIGRATION.md](BASH_TO_ZSH_MIGRATION.md).
