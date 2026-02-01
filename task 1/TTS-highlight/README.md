# TTS Highlight – Text-to-Speech with Real-time Highlighting

A React-based application that demonstrates synchronized Text-to-Speech (TTS) with real-time text highlighting for multiple Indian languages. This frontend-only solution enhances content accessibility and reading comprehension, particularly useful for educational content and digital learning platforms.

## 🚀 Features

- **Real-time Text Highlighting**: Words are highlighted as they are spoken for better comprehension
- **Multi-language Support**: Supports 6 Indian languages including English, Hindi, Gujarati, Marathi, Tamil, and Telugu
- **Responsive Design**: Works seamlessly on desktop and mobile devices
- **Speech Rate Control**: Adjustable speech speed from 0.5x to 2.0x
- **Cross-browser Compatibility**: Tested on Chrome, Firefox, Edge, Opera, and Safari
- **Content Display**: Shows sample text content from data files
- **Voice Support Notifications**: Toast alerts for unsupported voice fallbacks
- **Clean UI**: Modern Material-UI design with intuitive controls

## 🌐 Supported Languages

| Language | Code | Voice Support |
|----------|------|---------------|
| English | en-IN | ✅ Available |
| Hindi | hi-IN | ✅ Available |
| Gujarati | gu-IN | ✅ Available |
| Marathi | mr-IN | ✅ Available |
| Tamil | ta-IN | ✅ Available |
| Telugu | te-IN | ✅ Available |

## 🛠️ Technology Stack

- **React 19.2.3** - Modern hooks-based approach with functional components
- **Material-UI (MUI) 7.3.7** - UI components and theming system
- **Web Speech API** - Browser-native text-to-speech functionality
- **JavaScript ES6+** - Modern JavaScript features and syntax
- **Vite** - Fast development server and build tool

## 📱 Browser Compatibility

| Browser | Version | Status | Notes |
|---------|---------|--------|-------|
| Google Chrome | 90+ | ✅ Excellent | Full feature support |
| Mozilla Firefox | 85+ | ✅ Good | Some voice limitations |
| Microsoft Edge | 90+ | ✅ Excellent | Full feature support |
| Opera | 75+ | ✅ Good | Similar to Chrome |
| Safari | 14+ | ⚠️ Limited | Voice availability varies |

### Known Limitations

- **Safari**: Limited voice support for regional languages
- **Firefox**: May have fewer available voices for some languages
- **Mobile Browsers**: Voice availability depends on device and OS version
- **Regional Language Support**: Availability varies by browser and device

## 📋 Prerequisites

- Node.js 14+ and npm
- Modern web browser with Web Speech API support
- Internet connection for initial setup

## 🚀 Setup Instructions

### Step 1: Clone the Repository

```bash
git clone https://github.com/your-username/TTS-highlight.git
cd TTS-highlight
```

### Step 2: Install Dependencies

```bash
npm install
```

### Step 3: Start the Development Server

```bash
npm start
```

The application will open in your default browser at `http://localhost:5173`.

### Step 4: Build for Production

```bash
npm run build
```

The production build will be created in the `dist` folder.

## 🎯 How to Use

1. **Select Language**: Choose from the dropdown menu of supported languages
2. **Adjust Speech Rate**: Use the slider to control reading speed (0.5x - 2.0x)
3. **View Content**: Sample text is displayed based on selected language
4. **Controls**:
   - **Play**: Start reading the content with synchronized highlighting
   - **Pause**: Temporarily stop reading
   - **Resume**: Continue from where you paused
   - **Stop**: Stop reading and reset highlighting

## 🏗️ Project Structure

```
src/
├── components/
│   ├── Controls.jsx          # Playback control buttons
│   ├── FallbackAlert.jsx     # Browser compatibility alerts
│   ├── LanguageSelector.jsx  # Language selection dropdown
│   ├── TextHighlighter.jsx   # Text highlighting component
│   └── Toast.jsx            # Error notification popups
├── hooks/
│   └── useSpeechSynthesis.js # Custom hook for TTS functionality
├── data/
│   └── sampletxt.js         # Sample text content for different languages
├── App.jsx                  # Main application component
├── main.jsx                 # Application entry point
└── App.css                  # Application styles
```

## 🔧 Technical Implementation

### Text Highlighting Algorithm

The application uses a sophisticated word boundary detection system:

1. **Pre-calculation**: Word boundaries are calculated before speech starts
2. **Boundary Events**: Uses Web Speech API's `onboundary` events for precise timing
3. **Fallback Timer**: Timer-based highlighting for non-English languages
4. **State Management**: React hooks manage highlighting state efficiently

### Voice Support Detection

- **Language Matching**: Checks for exact and prefix-based voice matches
- **Fallback System**: Graceful degradation when specific voices are unavailable
- **User Notifications**: Toast alerts inform users about voice support issues
- **Auto-dismissal**: Notifications automatically disappear after 3 seconds

### Mobile Responsiveness

- **Breakpoint System**: Uses Material-UI's responsive design system
- **Touch-friendly Controls**: Optimized button sizes for mobile interaction
- **Adaptive Layout**: Responsive design for different screen sizes
- **Performance**: Optimized rendering for mobile devices

## 🧪 Testing

### Manual Testing Checklist

- [ ] Text highlighting syncs with speech for all supported languages
- [ ] Play/Pause/Resume/Stop controls work correctly
- [ ] Speech rate adjustment affects playback speed
- [ ] Language switching updates voice and text content
- [ ] Toast notifications appear for unsupported voices
- [ ] Responsive design works on mobile devices
- [ ] Graceful fallback for unavailable voices
- [ ] Cross-browser compatibility testing

### Browser Testing

Test the application on different browsers to ensure compatibility:

1. **Chrome**: Should work perfectly with all features
2. **Firefox**: Test voice availability for regional languages
3. **Edge**: Should work similarly to Chrome
4. **Safari**: Test on macOS and iOS devices
5. **Mobile**: Test on Android and iOS browsers

## 🚨 Troubleshooting

### Common Issues

1. **Voice Not Available**:
   - Toast notification will appear with fallback information
   - Try a different language
   - Check browser settings for speech synthesis
   - Update browser to latest version

2. **Highlighting Not Syncing**:
   - Refresh the page and try again
   - Check browser console for errors
   - Ensure content is loaded properly

3. **Mobile Issues**:
   - Ensure device supports Web Speech API
   - Check browser permissions
   - Try different mobile browser

### Debug Mode

Open browser console to see:
- Voice loading status
- Boundary event logs
- Error messages and warnings

## 📄 License

This project is part of a development task and is intended for demonstration purposes.

## 🤝 Contributing

This is a standalone development project. For issues or questions, please refer to the project documentation.

## 📊 Project Features

- ✅ **Synchronized TTS**: Text-to-speech with real-time highlighting
- ✅ **Modern React**: Hooks-based architecture with functional components
- ✅ **Material-UI**: Professional UI components and theming
- ✅ **Multi-language**: Support for 6 Indian languages
- ✅ **Cross-browser**: Compatible with major web browsers
- ✅ **Mobile-friendly**: Responsive design for all devices
- ✅ **Error Handling**: Graceful fallbacks and user notifications
- ✅ **Performance**: Optimized rendering and state management

## 🎯 Expected Outcome

A production-quality frontend application demonstrating synchronized Text-to-Speech with real-time text highlighting across multiple languages, browsers, and devices, providing an accessible and user-friendly reading experience for diverse content types.
