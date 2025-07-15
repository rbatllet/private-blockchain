#!/usr/bin/env zsh

# 🎯 Three-Tier API Demo - Showcase all three blockchain search APIs
# Demonstrates UserFriendlyEncryptionAPI, SearchSpecialistAPI, and SearchFrameworkEngine

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions
source "${SCRIPT_DIR}/lib/common_functions.zsh"

DEMO_TITLE="🚀 Three-Tier Blockchain API Demo"

print_header() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════════════════════════════════╗"
    echo "║                            🚀 THREE-TIER API DEMO                                   ║"
    echo "║                                                                                      ║"
    echo "║  This demo showcases all three blockchain APIs:                                     ║"
    echo "║  📊 UserFriendlyEncryptionAPI    - For 90% of developers (complete operations)     ║"
    echo "║  ⚡ SearchSpecialistAPI           - For search specialists (advanced search)        ║"
    echo "║  🔧 SearchFrameworkEngine         - For framework builders (maximum control)        ║"
    echo "║                                                                                      ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════════════╝"
    echo ""
}

print_tier_intro() {
    local tier="$1"
    local description="$2"
    local audience="$3"
    
    echo ""
    echo "┌─────────────────────────────────────────────────────────────────────────────────────┐"
    echo "│ $tier"
    echo "│ $description"
    echo "│ Target: $audience"
    echo "└─────────────────────────────────────────────────────────────────────────────────────┘"
    echo ""
}

run_tier1_demo() {
    print_tier_intro "📊 TIER 1: UserFriendlyEncryptionAPI" \
                     "Complete blockchain operations - storage, search, encryption, key management" \
                     "90% of developers - medical, financial, business applications"
    
    echo "ℹ️  Running UserFriendlyEncryptionAPI Demo..."
    echo ""
    
    mvn exec:java -Dexec.mainClass="demo.UserFriendlyEncryptionDemo" -q 2>&1 | while IFS= read -r line; do
        if [[ "$line" =~ "SUCCESS:|✅" ]]; then
            echo "✅ $line"
        elif [[ "$line" =~ "ERROR:|❌" ]]; then
            echo "❌ $line"
        elif [[ "$line" =~ "Patient|Medical|Account|Financial" ]]; then
            echo "🏥 $line"
        elif [[ "$line" =~ "🔒|🔐|🔑" ]]; then
            echo "$line"
        else
            echo "$line"
        fi
    done
    
    echo ""
    echo "📋 Tier 1 Summary:"
    echo "   ✅ Complete blockchain solution out of the box"
    echo "   ✅ Automatic key management and user creation"
    echo "   ✅ Built-in encryption for all sensitive data"
    echo "   ✅ Simple, intuitive API for common operations"
    echo "   ✅ Enterprise-ready security and validation"
    echo ""
}

run_tier2_demo() {
    print_tier_intro "⚡ TIER 2: SearchSpecialistAPI" \
                     "Specialized search operations with advanced features and analytics" \
                     "Search specialists - analytics, discovery tools, search optimization"
    
    echo "ℹ️  Running SearchSpecialistAPI Demo (via SearchFrameworkDemo)..."
    echo ""
    
    echo "📊 SearchSpecialistAPI provides:"
    echo "   ⚡ Multiple search strategies (fast, secure, intelligent)"
    echo "   📈 Real-time performance metrics and analytics"
    echo "   🔍 Advanced search modes (public, encrypted, hybrid)"
    echo "   🎯 Automatic strategy selection based on query complexity"
    echo "   📊 Comprehensive diagnostics and capability reporting"
    echo ""
    
    echo "🎬 Running actual SearchFrameworkEngine demonstration:"
    echo "═══════════════════════════════════════════════════════════════════════════════════════"
    
    mvn exec:java -Dexec.mainClass="demo.SearchFrameworkDemo" -q 2>&1 | while IFS= read -r line; do
        if [[ "$line" =~ "SEARCH ENGINE|ENGINE DEMO" ]]; then
            echo "🔧 $line"
        elif [[ "$line" =~ "Fast search|Simple search|Secure search|Intelligent search" ]]; then
            echo "⚡ $line"
        elif [[ "$line" =~ "Performance|Statistics|Metrics|diagnostics" ]]; then
            echo "📊 $line"
        elif [[ "$line" =~ "SUCCESS:|✅" ]]; then
            echo "✅ $line"
        elif [[ "$line" =~ "ERROR:|❌" ]]; then
            echo "❌ $line"
        elif [[ "$line" =~ "Setting up|Initializing" ]]; then
            echo "🔧 $line"
        else
            echo "$line"
        fi
    done
    
    echo "═══════════════════════════════════════════════════════════════════════════════════════"
    echo ""
    echo "📋 Tier 2 Summary:"
    echo "   ⚡ Sub-50ms public searches for maximum performance"
    echo "   🔐 Advanced encrypted content search with analytics"
    echo "   🎯 Intelligent routing for optimal search strategy"
    echo "   📊 Comprehensive performance metrics and diagnostics"
    echo "   🔄 Multiple search capabilities in a single API"
    echo ""
}

run_tier3_demo() {
    print_tier_intro "🔧 TIER 3: SearchFrameworkEngine" \
                     "Low-level engine for building custom blockchain search solutions" \
                     "Framework developers - custom search engines, specialized applications"
    
    echo "ℹ️  Running SearchFrameworkEngine Demo..."
    echo ""
    
    echo "🔧 SearchFrameworkEngine provides:"
    echo "   🏗️ Direct control over indexing strategies"
    echo "   🎛️ Custom metadata layer management"
    echo "   🔀 Flexible search strategy routing"
    echo "   🚀 Raw performance optimization"
    echo "   🔌 Custom integration points"
    echo "   ⚙️ Full access to engine internals"
    echo ""
    
    echo "🎬 Running actual SearchFrameworkEngine demonstration:"
    echo "═══════════════════════════════════════════════════════════════════════════════════════"
    
    mvn exec:java -Dexec.mainClass="demo.SearchFrameworkDemo" -q 2>&1 | while IFS= read -r line; do
        if [[ "$line" =~ "SEARCH ENGINE|ENGINE DEMO" ]]; then
            echo "🔧 $line"
        elif [[ "$line" =~ "indexing|Indexing" ]]; then
            echo "📦 $line"
        elif [[ "$line" =~ "Strategy|strategy" ]]; then
            echo "🎯 $line"
        elif [[ "$line" =~ "Performance|Statistics|Metrics" ]]; then
            echo "📊 $line"
        elif [[ "$line" =~ "Engine|engine" ]]; then
            echo "🔧 $line"
        elif [[ "$line" =~ "SUCCESS:|✅" ]]; then
            echo "✅ $line"
        elif [[ "$line" =~ "ERROR:|❌" ]]; then
            echo "❌ $line"
        else
            echo "$line"
        fi
    done
    
    echo "═══════════════════════════════════════════════════════════════════════════════════════"
    echo ""
    echo "📋 Tier 3 Summary:"
    echo "   🔧 Maximum control and flexibility"
    echo "   🏗️ Build custom search solutions"
    echo "   ⚙️ Direct access to all engine components"
    echo "   🚀 Optimal performance through fine-tuning"
    echo "   🔌 Extensible architecture for integrations"
    echo "   🎛️ Full customization of search behavior"
    echo ""
}

show_comparison_matrix() {
    echo ""
    echo "┌─────────────────────────────────────────────────────────────────────────────────────┐"
    echo "│                              📊 API COMPARISON MATRIX                               │"
    echo "├─────────────────────────┬─────────────────────┬─────────────────────┬──────────────┤"
    echo "│ Feature                 │ UserFriendlyAPI     │ SearchSpecialistAPI │ Framework     │"
    echo "├─────────────────────────┼─────────────────────┼─────────────────────┼──────────────┤"
    echo "│ Target Audience         │ 90% of developers   │ Search specialists  │ Framework devs│"
    echo "│ Learning Curve          │ Easy (1-2 hours)    │ Moderate (1-2 days) │ Steep (1 week)│"
    echo "│ Key Management          │ ✅ Automatic        │ ❌ Manual           │ ❌ Manual     │"
    echo "│ Encryption              │ ✅ Built-in         │ ⚠️  Optional        │ ⚠️  Optional  │"
    echo "│ Search Strategies       │ ✅ Auto-selected    │ ✅ Manual choice    │ ✅ Full control│"
    echo "│ Performance             │ Good                │ Better              │ Best          │"
    echo "│ Customization           │ Limited             │ Moderate            │ Full          │"
    echo "│ Use Cases              │ Apps, Services      │ Analytics, Tools    │ Frameworks    │"
    echo "└─────────────────────────┴─────────────────────┴─────────────────────┴──────────────┘"
    echo ""
}

show_decision_tree() {
    echo ""
    echo "┌─────────────────────────────────────────────────────────────────────────────────────┐"
    echo "│                              🎯 WHICH API SHOULD I USE?                             │"
    echo "├─────────────────────────────────────────────────────────────────────────────────────┤"
    echo "│                                                                                     │"
    echo "│  Are you building a medical, financial, or business application?                   │"
    echo "│  └─ YES → Use UserFriendlyEncryptionAPI ✅                                        │"
    echo "│                                                                                     │"
    echo "│  Do you need advanced search features with performance analytics?                   │"
    echo "│  └─ YES → Use SearchSpecialistAPI ⚡                                               │"
    echo "│                                                                                     │"
    echo "│  Are you building a custom search framework or engine?                             │"
    echo "│  └─ YES → Use SearchFrameworkEngine 🔧                                             │"
    echo "│                                                                                     │"
    echo "│  Not sure? → Start with UserFriendlyEncryptionAPI! 📊                             │"
    echo "│                                                                                     │"
    echo "└─────────────────────────────────────────────────────────────────────────────────────┘"
    echo ""
}

show_getting_started_links() {
    echo ""
    echo "┌─────────────────────────────────────────────────────────────────────────────────────┐"
    echo "│                              📚 GETTING STARTED GUIDES                              │"
    echo "├─────────────────────────────────────────────────────────────────────────────────────┤"
    echo "│                                                                                     │"
    echo "│  📊 General Developers:                                                             │"
    echo "│     • docs/GETTING_STARTED.md                                                      │"
    echo "│     • docs/EXAMPLES.md                                                             │"
    echo "│                                                                                     │"
    echo "│  ⚡ Search Specialists:                                                             │"
    echo "│     • docs/SEARCH_FRAMEWORK_GUIDE.md                                               │"
    echo "│     • docs/SEARCH_COMPARISON.md                                                    │"
    echo "│                                                                                     │"
    echo "│  🔧 Framework Developers:                                                           │"
    echo "│     • docs/TECHNICAL_DETAILS.md                                                    │"
    echo "│     • docs/API_GUIDE.md                                                            │"
    echo "│                                                                                     │"
    echo "│  📋 Complete Comparison:                                                            │"
    echo "│     • docs/SEARCH_APIS_COMPARISON.md                                               │"
    echo "└─────────────────────────────────────────────────────────────────────────────────────┘"
    echo ""
}

main() {
    print_header
    
    # Check prerequisites
    if ! check_java || ! check_maven; then
        exit 1
    fi
    
    # Clean and compile
    cleanup_database
    
    if ! compile_project; then
        exit 1
    fi
    
    echo "🎯 This demo will showcase all three APIs in sequence..."
    echo "ℹ️  Each tier serves different audiences and use cases"
    echo ""
    
    # Run all three tiers
    run_tier1_demo
    run_tier2_demo  
    run_tier3_demo
    
    # Show comprehensive comparison
    show_comparison_matrix
    show_decision_tree
    show_getting_started_links
    
    # Show practical next steps
    show_next_steps
    
    echo "🎉 Three-Tier API Demo completed!"
    echo "ℹ️  Choose the API that best fits your needs and expertise level"
    echo ""
}

show_next_steps() {
    echo ""
    echo "┌───────────────────────────────────────────────────────────────────────────────────────┐"
    echo "│                              🚀 NEXT STEPS TO GET STARTED                              │"
    echo "├───────────────────────────────────────────────────────────────────────────────────────┤"
    echo "│                                                                                     │"
    echo "│  📊 If you chose UserFriendlyEncryptionAPI:                                          │"
    echo "│     1. ./scripts/run_user_friendly_encryption_demo.zsh                            │"
    echo "│     2. Read docs/GETTING_STARTED.md                                               │"
    echo "│     3. Try the examples in docs/EXAMPLES.md                                       │"
    echo "│                                                                                     │"
    echo "│  ⚡ If you chose SearchSpecialistAPI:                                                │"
    echo "│     1. ./scripts/run_search_framework_demo.zsh                                    │"
    echo "│     2. Read docs/SEARCH_FRAMEWORK_GUIDE.md                                        │"
    echo "│     3. Review docs/SEARCH_COMPARISON.md                                           │"
    echo "│                                                                                     │"
    echo "│  🔧 If you chose SearchFrameworkEngine:                                             │"
    echo "│     1. ./scripts/run_search_framework_demo.zsh                                    │"
    echo "│     2. Read docs/TECHNICAL_DETAILS.md                                             │"
    echo "│     3. Study docs/API_GUIDE.md                                                    │"
    echo "│                                                                                     │"
    echo "│  📋 Other useful demos:                                                           │"
    echo "│     • ./scripts/run_exhaustive_search_demo.zsh                                    │"
    echo "│     • ./scripts/run_blockchain_demo.zsh                                           │"
    echo "│     • ./scripts/run_simple_demo.zsh                                               │"
    echo "│                                                                                     │"
    echo "│  📝 Run all tests: ./scripts/run_all_tests.zsh                                    │"
    echo "└───────────────────────────────────────────────────────────────────────────────────────┘"
    echo ""
}

# Execute main function
main "$@"