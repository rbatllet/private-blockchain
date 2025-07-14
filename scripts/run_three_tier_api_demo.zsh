#!/usr/bin/env zsh

# 🎯 Three-Tier API Demo - Showcase all three blockchain search APIs
# Demonstrates UserFriendlyEncryptionAPI, SearchSpecialistAPI, and SearchFrameworkEngine

# Load common functions
source "$(dirname "$0")/lib/common_functions.zsh"

DEMO_TITLE="🚀 Three-Tier Blockchain API Demo"
JAR_PATH="target/privateBlockchain-1.0-SNAPSHOT.jar"

print_header() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════════════════════════════════╗"
    echo "║                            🚀 THREE-TIER API DEMO                                   ║"
    echo "║                                                                                      ║"
    echo "║  This demo showcases all three blockchain APIs:                                     ║"
    echo "║  📊 UserFriendlyEncryptionAPI    - For 90% of developers (complete operations)     ║"
    echo "║  ⚡ SearchSpecialistAPI - For search specialists (advanced search)        ║"
    echo "║  🔧 SearchFrameworkEngine    - For framework builders (maximum control)        ║"
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
    
    java -cp "$JAR_PATH" demo.UserFriendlyEncryptionDemo 2>&1 | while IFS= read -r line; do
        if [[ "$line" =~ "SUCCESS:|✅" ]]; then
            echo "✅ $line"
        elif [[ "$line" =~ "ERROR:|❌" ]]; then
            echo "❌ $line"
        elif [[ "$line" =~ "INFO:|ℹ️" ]]; then
            echo "ℹ️  $line"
        elif [[ "$line" =~ "DEMO:|🎯" ]]; then
            echo "🎯 $line"
        else
            echo "$line"
        fi
    done
    
    echo ""
    echo "📋 Tier 1 Summary:"
    echo "   ✅ Complete blockchain operations in simple API"
    echo "   ✅ Automatic encryption and key management"
    echo "   ✅ Built-in search with intelligent optimization"
    echo "   ✅ Enterprise-ready security and validation"
    echo ""
}

run_tier2_demo() {
    print_tier_intro "⚡ TIER 2: SearchSpecialistAPI" \
                     "Specialized search operations with advanced features and analytics" \
                     "Search specialists - analytics, discovery tools, search optimization"
    
    echo "ℹ️  Running SearchSpecialistAPI Demo..."
    echo ""
    
    java -cp "$JAR_PATH" demo.SearchFrameworkDemo 2>&1 | while IFS= read -r line; do
        if [[ "$line" =~ "Fast search|Simple search|Secure search|Intelligent search" ]]; then
            echo "⚡ $line"
        elif [[ "$line" =~ "Performance|Statistics|Metrics" ]]; then
            echo "📊 $line"
        elif [[ "$line" =~ "SUCCESS:|✅" ]]; then
            echo "✅ $line"
        elif [[ "$line" =~ "ERROR:|❌" ]]; then
            echo "❌ $line"
        else
            echo "$line"
        fi
    done
    
    echo ""
    echo "📋 Tier 2 Summary:"
    echo "   ⚡ Sub-50ms public searches for maximum performance"
    echo "   🔐 Advanced encrypted content search with analytics"
    echo "   🧠 Intelligent search with automatic strategy selection"
    echo "   📊 Comprehensive search metrics and diagnostics"
    echo ""
}

run_tier3_demo() {
    print_tier_intro "🔧 TIER 3: SearchFrameworkEngine" \
                     "Low-level search engine with direct strategy control and custom configuration" \
                     "Framework builders - custom search algorithms, specialized implementations"
    
    echo "ℹ️  Running SearchFrameworkEngine Demo..."
    echo ""
    
    java -cp "$JAR_PATH" demo.ExhaustiveSearchDemo 2>&1 | while IFS= read -r line; do
        if [[ "$line" =~ "Strategy|Engine|Low-level|Direct" ]]; then
            echo "🔧 $line"
        elif [[ "$line" =~ "Exhaustive|TRUE|Off-chain" ]]; then
            echo "🔍 $line"
        elif [[ "$line" =~ "SUCCESS:|✅" ]]; then
            echo "✅ $line"
        elif [[ "$line" =~ "ERROR:|❌" ]]; then
            echo "❌ $line"
        else
            echo "$line"
        fi
    done
    
    echo ""
    echo "📋 Tier 3 Summary:"
    echo "   🔧 Direct access to search strategies and engine configuration"
    echo "   🔍 TRUE exhaustive search across on-chain and off-chain data"
    echo "   ⚙️  Custom encryption configuration and security policies"
    echo "   🎛️  Maximum control for specialized search implementations"
    echo ""
}

show_comparison_matrix() {
    echo ""
    echo "┌─────────────────────────────────────────────────────────────────────────────────────┐"
    echo "│                               📊 API COMPARISON MATRIX                              │"
    echo "├─────────────────────────────────┬─────────────────┬─────────────────┬─────────────────┤"
    echo "│ Feature                         │ UserFriendly    │ Search Specialist│ Search Engine   │"
    echo "├─────────────────────────────────┼─────────────────┼─────────────────┼─────────────────┤"
    echo "│ Target Audience                 │ 90% developers  │ Search experts  │ Framework devs  │"
    echo "│ Complexity Level                │ ✅ Low          │ ⚡ Medium        │ 🔧 High         │"
    echo "│ Data Storage                    │ ✅ Complete     │ ❌ No           │ ❌ No           │"
    echo "│ Encryption Management          │ ✅ Automatic    │ ❌ Manual       │ ❌ Manual       │"
    echo "│ Key Management                  │ ✅ Built-in     │ ❌ External     │ ❌ External     │"
    echo "│ Basic Search                    │ ✅ Simple       │ ✅ Advanced     │ 🔧 Expert       │"
    echo "│ Performance Tuning              │ ⚡ Good         │ ⚡ Excellent    │ 🔧 Manual       │"
    echo "│ Custom Strategies               │ ❌ No           │ ⚡ Limited      │ ✅ Full         │"
    echo "│ Learning Curve                  │ 📚 Easy         │ 📚 Medium       │ 📚 Steep        │"
    echo "└─────────────────────────────────┴─────────────────┴─────────────────┴─────────────────┘"
    echo ""
}

show_decision_tree() {
    echo ""
    echo "┌─────────────────────────────────────────────────────────────────────────────────────┐"
    echo "│                              🎯 WHICH API TO CHOOSE?                                │"
    echo "│                                                                                     │"
    echo "│  Building a complete blockchain application?                                        │"
    echo "│  ├─ YES → Use UserFriendlyEncryptionAPI ✅                                          │"
    echo "│  └─ NO                                                                              │"
    echo "│      └─ Need only search functionality?                                             │"
    echo "│          ├─ YES → Use SearchSpecialistAPI ⚡                              │"
    echo "│          └─ NO → Building framework/custom engine?                                  │"
    echo "│              ├─ YES → Use SearchFrameworkEngine 🔧                             │"
    echo "│              └─ NO → Use UserFriendlyEncryptionAPI ✅ (safest choice)              │"
    echo "│                                                                                     │"
    echo "│  💡 TIP: You can combine APIs! Use UserFriendlyEncryptionAPI for storage           │"
    echo "│          and SearchSpecialistAPI for specialized search operations.      │"
    echo "└─────────────────────────────────────────────────────────────────────────────────────┘"
    echo ""
}

show_getting_started_links() {
    echo ""
    echo "┌─────────────────────────────────────────────────────────────────────────────────────┐"
    echo "│                              📚 GETTING STARTED LINKS                              │"
    echo "│                                                                                     │"
    echo "│  🚀 Start Here (90% of developers):                                                │"
    echo "│     • docs/USER_FRIENDLY_SEARCH_GUIDE.md                                           │"
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
    
    # Check if JAR exists
    if [[ ! -f "$JAR_PATH" ]]; then
        echo "❌ JAR file not found: $JAR_PATH"
        echo "ℹ️  Please run 'mvn clean package -DskipTests' first"
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
    
    echo "🎉 Three-Tier API Demo completed!"
    echo "ℹ️  Choose the API that best fits your needs and expertise level"
    echo ""
}

# Execute main function
main "$@"