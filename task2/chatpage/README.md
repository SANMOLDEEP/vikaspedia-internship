# Chat with Page Feature Integration

A ReactJS application featuring an interactive chatbot sidebar that allows users to ask questions about the current page content. The AI responds strictly based only on the text available on that page.

##  Project Overview

This project implements a complete "Chat with Page" feature that enables users to interact with an AI assistant that has deep knowledge of the specific content on the current page. The implementation includes:

- **Floating Action Button**: Fixed to bottom-right corner with smooth animations
- **Chat Sidebar**: Slides in from the right with message history and input area  
- **Dynamic Content Extraction**: Automatically extracts text from the main content area
- **AI-Powered Responses**: Uses Google Gemini API for intelligent responses
- **Responsive Design**: Works seamlessly on desktop and mobile devices
- **Error Handling**: User-friendly error messages for API failures
- **Loading States**: Visual feedback during API calls
- **Auto-Model Selection**: Intelligently selects the best available Gemini model

##  AI API Selection

### Chosen API: Google Gemini API

**Reasons for selection:**
1. **Free Tier Available**: Generous free tier with 60 requests per minute
2. **High Quality Responses**: Advanced language model with excellent comprehension
3. **Easy Integration**: Simple REST API with clear documentation
4. **Context-Aware**: Handles long context well for page content analysis
5. **Reliable**: Backed by Google's infrastructure
6. **Model Variety**: Supports multiple Gemini models with auto-selection

### Alternative Options Considered:
- **Hugging Face Inference API**: Good but more complex setup
- **OpenRouter**: Multiple models but requires more configuration
- **Sarvam.AI**: Limited free tier capabilities

##  API Key Setup Instructions

### Step 1: Get Google Gemini API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Choose an existing Google Cloud project or create a new one
5. Copy the generated API key

### Step 2: Configure the Application

1. Open `src/components/ChatSidebar.jsx`
2. Find the line: `const API_KEY = 'INSERT_KEY_HERE'`
3. Replace `INSERT_KEY_HERE` with your actual API key

**Important**: Never commit your actual API key to version control. The placeholder `INSERT_KEY_HERE` is used for security.

### Step 3: Environment Variables (Recommended)

For better security, you can use environment variables:

1. Create a `.env` file in the project root:
```
VITE_GEMINI_API_KEY=your_actual_api_key_here
```

2. Update `ChatSidebar.jsx` to use the environment variable:
```javascript
const API_KEY = import.meta.env.VITE_GEMINI_API_KEY || 'INSERT_KEY_HERE'
```

3. Add `.env` to your `.gitignore` file

## Installation and Setup

### Prerequisites
- Node.js (version 16 or higher)
- npm or yarn

### Installation Steps

1. **Clone the repository**:
```bash
git clone <repository-url>
cd chatpage
```

2. **Install dependencies**:
```bash
npm install
```

3. **Configure API Key** (see API Key Setup Instructions above)

4. **Start the development server**:
```bash
npm run dev
```

5. **Open your browser** and navigate to `http://localhost:5173`

##  Usage

1. **Open Chat**: Click the floating chat button in the bottom-right corner
2. **Ask Questions**: Type questions about the page content and press Enter
3. **View Responses**: AI responses will appear in the chat sidebar

## Project Structure

```
src/
├── components/
│   ├── ChatButton.jsx          # Floating action button
│   ├── ChatSidebar.jsx         # Main chat component with AI integration
│   └── ChatSidebar.css         # Styles for the chat sidebar
├── App.jsx                     # Main application with sample content
├── App.css                     # Application styles
└── main.jsx                    # Application entry point
```

## Key Features Implementation

### Dynamic Content Extraction
- Extracts text from `#mainContent` element
- Removes HTML tags and cleans whitespace
- Handles missing content gracefully

### AI Integration
- Auto-selects best available Gemini model
- Context-aware prompts with strict instructions
- Comprehensive error handling and fallback mechanisms

### User Experience
- Smooth CSS animations and transitions
- Visual loading indicators during API calls
- Responsive design for all screen sizes
- Keyboard shortcuts (Enter to send, Shift+Enter for new line)
- Clear chat function to reset conversation

##  Functional Requirements Met

###  Entry Point - Floating Action Button
- Fixed to bottom-right corner with chat icon
- Click toggles chat sidebar visibility

### Chat Sidebar
- Off-canvas design sliding from right (350-400px wide)
- Header with "Page Assistant" title and close button
- Scrollable message area with distinct user/AI styling
- Fixed input area with send button

### User Interaction Flow
- Click chat button → sidebar opens
- Type question → press Enter or click send
- Loading state shows "Thinking..." with animated dots
- AI response appears in chat window

### Dynamic Context Extraction
- Extracts text from `#mainContent` element
- Removes HTML tags and cleans whitespace
- Provides clean text to AI for context-based responses

### AI Model Integration
- Google Gemini API with free tier
- Auto-selects best available model
- Context-aware prompt engineering
- Handles API errors gracefully

## Browser Compatibility
- Chrome, Firefox, Safari, Edge (latest)

## Security Notes
- API keys should never be committed to version control
- Content extraction is client-side only
- No external data storage or tracking
- CORS-compliant API calls
- Input sanitization for user queries

## Development
### Available Scripts
- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## License
This project is open source and available under the [MIT License](LICENSE).