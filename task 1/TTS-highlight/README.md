# TTS Highlight — Text-to-Speech with Word Highlighting

A React app that reads text out loud and highlights words as they are spoken. It works with different Indian languages and uses your browser's built-in voice system.

## Table of Contents
- [About This Project](#about-this-project)
- [Main Features](#main-features)
- [Languages We Support](#languages-we-support)
- [Technologies Used](#technologies-used)
- [Browser Support](#browser-support)
- [What You Need](#what-you-need)
- [How to Set Up](#how-to-set-up)
- [Installing Voices](#installing-voices)
- [How to Use](#how-to-use)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Voice Selection](#voice-selection)
- [Known Issues](#known-issues)
- [Help & Support](#help--support)
- [Developer](#developer)
- [License](#license)

## About This Project

This is a simple web app that reads text and highlights each word as it's spoken. It's made to help people learn and understand better, especially for Indian languages.

## Main Features

- **Word Highlighting**: Words light up as they are spoken
- **Multiple Languages**: Works with English and Hindi
- **Edit Text**: You can change the text while keeping all features working
- **Playback Controls**: Play, Pause, Resume, and Stop buttons
- **Speed Control**: Make speech faster or slower (0.5x to 2.0x)
- **Sample Texts**: Comes with example texts for each language
- **Works on All Devices**: Good for phones and computers
- **Auto Voice Detection**: Finds the best voice automatically

## Languages We Support

| Language | Code | Status | Notes |
|----------|-------|--------|-------|
| English (US) | `en-US` | Ready to Use | Works in most browsers |
| English (India) | `en-IN` | Ready to Use | Works in most browsers |
| Hindi | `hi-IN` | Ready to Use | Better if you install the voice |

## Technologies Used

- **React 18.2.0**: Modern JavaScript for building the app
- **Material-UI 5.15.0**: Nice looking buttons and layouts
- **Web Speech API**: Browser's built-in text-to-speech
- **JavaScript ES6+**: Modern JavaScript features
- **Vite 5.0.8**: Fast tool for building and running the app

## Browser Support

| Browser | Computer | Phone | How Well It Works |
|----------|-----------|--------|-----------------|
| Chrome | Full Support | Full Support | Works Best |
| Edge | Full Support | Full Support | Works Great |
| Firefox | Full Support | Full Support | Works Well |
| Safari | Full Support | Some Support | Voice quality varies |
| Opera | Full Support | Full Support | Like Chrome |

## What You Need

- **Node.js**: Version 14 or newer
- **Modern Browser**: Chrome, Firefox, Edge, or Safari
- **Internet**: For downloading voices if needed

## How to Set Up

### Get the Code

```bash
git clone https://github.com/SANMOLDEEP/vikaspedia-internship/tree/main/task%201/TTS-highlight
cd TTS-highlight
```

### Install Packages

```bash
npm install
```

### Start the App

```bash
npm run dev
```

Open your browser and go to: http://localhost:5173

### Build for Live Use

```bash
npm run build
```

### Test the Live Version

```bash
npm run preview
```

## Installing Voices

### On Windows (10/11)

1. Go to Settings → Time & Language → Language
2. Click **Add a language**
3. Find and choose your language (like Hindi)
4. Turn on **Text-to-speech** while installing
5. Finish install and restart your browser

### On Mac

1. Go to System Preferences → Accessibility → Spoken Content
2. Click **System Voice** → **Manage Voices**
3. Download the voice you need
4. Restart your browser

## How to Use

- **Pick Language**: Choose from the dropdown menu
- **Set Speed**: Use the slider (0.5x to 2.0x)
- **Edit Text**: Click Edit button, type your text, then Save or Cancel
- **See Text**: Sample text shows for your chosen language
- **Start Reading**: Click Play to begin with highlighting
- **Control Reading**: Use Pause/Resume/Stop as needed

## Project Structure

```
src/
├── components/
│   ├── Controls.jsx          # Play/Pause/Resume/Stop buttons
│   ├── FallbackAlert.jsx     # Shows if browser doesn't work
│   ├── LanguageSelector.jsx  # Language dropdown menu
│   └── TextHighlighter.jsx   # Highlights words while speaking
├── hooks/
│   └── useSpeechSynthesis.js # Text-to-speech logic
├── data/
│   └── sampletxt.js         # Sample texts for each language
├── App.jsx                  # Main app component
├── main.jsx                 # App starting point
└── App.css                  # App styles
```

## How It Works

1. You choose a language from the menu
2. The app finds voices in your browser
3. You can edit the text if you want
4. Click Play to start reading
5. The app creates speech with the right voice and speed
6. For English and Hindi: Uses browser events for exact highlighting
7. For other languages: Uses a timer for highlighting
8. You can Pause, Resume, or Stop anytime

## Voice Selection

1. **Exact Match**: Looks for voice with the same language code (like `hi-IN`)
2. **Language Match**: Looks for voice with the same language (like `hi`)
3. **First Available**: Uses any voice if no match is found
4. **Show Alert**: Tells you if no voice works

## Known Issues

- **Regional Languages**: May not work in all browsers/phones
- **Highlighting**: Works best for English and Hindi
- **Mobile Chrome**: Some highlighting issues on phones
- **Safari**: Limited voice options for some languages
- **Voice Quality**: Depends on your browser and system

## Help & Support

- **No Voice**: Install the language voice and restart browser
- **Highlighting Not Working**: Refresh the page, try another browser
- **Phone Issues**: Use Chrome, check if voice is installed
- **Voices Not Loading**: Wait a few seconds or refresh the page

## Developer

**Made by**: Anmoldeep Singh

## License

This project is for learning and demonstration purposes.
