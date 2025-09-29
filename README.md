# 🔥 The Forge - Figma to Compose Parity Plugin

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![IntelliJ Platform](https://img.shields.io/badge/IntelliJ%20Platform-2023.3+-blue.svg)](https://plugins.jetbrains.com/docs/intellij/welcome.html)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-purple.svg)](https://kotlinlang.org/)

A powerful Android Studio plugin that automatically verifies your Compose UI components match their Figma designs with pixel-perfect accuracy and structural validation.

## 🚀 Features

- **Visual Parity Testing**: Compare rendered Compose components with Figma designs using perceptual hashing
- **Structural Validation**: Verify layout properties, spacing, and design tokens match exactly
- **Secure Integration**: Safe storage of Figma Personal Access Tokens using IntelliJ's PasswordSafe API
- **Real-time Feedback**: Instant visual and structural comparison results in IDE
- **Baseline Management**: Approve design changes and update baselines for future comparisons

## 🛠️ Architecture

The plugin is built on the IntelliJ Platform and follows a modular architecture:

### Core Services
- **ForgeService**: Main orchestration service
- **FigmaService**: Figma API integration and data acquisition
- **ComparisonService**: Visual and structural comparison engine
- **BaselineService**: Baseline management and storage

### Key Components
- **ComposableAnalyzer**: Analyzes Kotlin Composable functions
- **ConfigurationStorage**: Persistent settings management
- **ComparisonDialog**: UI for initiating comparisons
- **ForgeToolWindow**: Results display and management

## 🏗️ Development Setup

### Prerequisites
- IntelliJ IDEA 2023.3 or later
- Android Studio (for testing)
- JDK 17 or later
- Kotlin 1.9.20

### Building the Plugin

1. Clone the repository
2. Open in IntelliJ IDEA
3. Run the Gradle build:
   ```bash
   ./gradlew build
   ```

### Testing the Plugin

1. Build the plugin
2. Run the plugin in a sandbox environment:
   ```bash
   ./gradlew runIde
   ```

## 📋 Implementation Phases

### Phase 1: Project Setup ✅
- [x] IntelliJ Platform Plugin initialization
- [x] Core service architecture
- [x] Basic UI components
- [x] Configuration management

### Phase 2: Authentication (In Progress)
- [ ] Figma Personal Access Token storage
- [ ] Secure credential management
- [ ] Configuration UI

### Phase 3: Compose Render Capture
- [ ] Gradle task bridge
- [ ] Headless rendering framework
- [ ] Layout tree extraction

### Phase 4: Figma Data Acquisition
- [ ] Figma REST API integration
- [ ] MCP (Model Context Protocol) support
- [ ] Image and structure data fetching

### Phase 5: Comparison Engine
- [ ] Visual comparison (pHash)
- [ ] Structural comparison (tree diffing)
- [ ] Design token validation

### Phase 6: Reporting & UI
- [ ] Tool window implementation
- [ ] Visual difference display
- [ ] Baseline management UI

## 🔧 Configuration

The plugin can be configured through Android Studio Settings > Tools > The Forge:

- **Figma Credentials**: Personal Access Token and username
- **Comparison Thresholds**: Visual and structural similarity requirements
- **Behavior Settings**: Auto-capture, detailed reports, debug logging

## 📖 Usage

1. Right-click on any `@Composable @Preview` function
2. Select "Compare with Figma" from the context menu
3. Enter your Figma URL or node ID
4. Review the comparison results in the tool window
5. Approve changes as new baselines when needed

## 🤝 Contributing

This project follows the implementation plan outlined in the architectural blueprint. Each phase builds upon the previous one, ensuring a robust and maintainable codebase.

## 📄 License

[License information to be added]

## 📸 Screenshots

*Coming soon - Screenshots of the plugin in action will be added here*

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Clone your fork: `git clone https://github.com/yourusername/forge-figma-compose-parity.git`
3. Open in IntelliJ IDEA
4. Build the plugin: `./gradlew build`
5. Run in sandbox: `./gradlew runIde`

### Reporting Issues

Found a bug? Please [open an issue](https://github.com/yourusername/forge-figma-compose-parity/issues) with:
- Plugin version
- IntelliJ IDEA version
- Steps to reproduce
- Expected vs actual behavior

## 📋 Roadmap

- [ ] **Phase 1**: ✅ Plugin foundation and basic UI
- [ ] **Phase 2**: 🔄 Secure authentication and configuration
- [ ] **Phase 3**: ⏳ Compose render capture system
- [ ] **Phase 4**: ⏳ Figma data acquisition pipeline
- [ ] **Phase 5**: ⏳ Advanced comparison engine
- [ ] **Phase 6**: ⏳ Enhanced reporting and UI

## 🏆 Acknowledgments

- Built with [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- Inspired by the need for design-system consistency in Android development
- Thanks to the Figma team for their excellent API

## 🔗 Related

- [Figma API Documentation](https://www.figma.com/developers/api)
- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Studio](https://developer.android.com/studio)
