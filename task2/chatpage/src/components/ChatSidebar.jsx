import React, { useState, useRef, useEffect } from 'react'
import './ChatSidebar.css'

const ChatSidebar = ({ isOpen, onClose }) => {
  const [messages, setMessages] = useState([])
  const [inputValue, setInputValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const messagesEndRef = useRef(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const extractPageContent = () => {
    const mainContent = document.getElementById('mainContent')
    if (!mainContent) {
      return 'No main content found on this page.'
    }
    
    // Clone the element to avoid modifying the DOM
    const clone = mainContent.cloneNode(true)
    
    
    const scriptsAndStyles = clone.querySelectorAll('script, style')
    scriptsAndStyles.forEach(element => element.remove())
    
  
    let text = clone.textContent || clone.innerText || ''
    
  
    text = text.replace(/\s+/g, ' ').trim()
    
    return text
  }

  const [selectedModel, setSelectedModel] = useState('')

  // Fetch and select the best available model on component mount
  useEffect(() => {
    const fetchBestModel = async () => {
      const API_KEY = 'INSERT_KEY_HERE'
      const MODELS_URL = 'https://generativelanguage.googleapis.com/v1beta/models'

      try {
        const response = await fetch(`${MODELS_URL}?key=${API_KEY}`)
        if (response.ok) {
          const data = await response.json()
          const models = data.models || []
          
          // Filter for text generation models and prioritize
          const textModels = models
            .filter(model => 
              model.name.includes('gemini') && 
              (model.name.includes('generateContent') || model.supportedGenerationMethods?.includes('generateContent'))
            )
            .map(model => model.name)
            .sort((a, b) => {
              // Priority order: gemini-1.5-pro > gemini-pro > others
              const priority = {
                'gemini-1.5-pro': 1,
                'gemini-pro': 2,
                'gemini-pro-vision': 3
              }
              
              const aName = a.split('/').pop()
              const bName = b.split('/').pop()
              
              return (priority[aName] || 999) - (priority[bName] || 999)
            })

          // Select the best available model
          if (textModels.length > 0) {
            const bestModel = textModels[0]
            setSelectedModel(bestModel)
            console.log('Auto-selected best model:', bestModel)
          }
        }
      } catch (error) {
        console.error('Error fetching models:', error)
        // Fallback to gemini-pro if model fetching fails
        setSelectedModel('models/gemini-pro')
      }
    }

    fetchBestModel()
  }, [])

  const generateAIResponse = async (userQuestion, pageContent) => {
    const API_KEY = 'INSERT_KEY_HERE'
    
    // Use the selected model or fallback to gemini-pro
    const modelToUse = selectedModel || 'models/gemini-pro'
    const API_URL = `https://generativelanguage.googleapis.com/v1beta/${modelToUse}:generateContent`

    const prompt = `You are a helpful assistant that answers questions based ONLY on the provided context. 
Do not use any external knowledge. If the answer cannot be found in the context, say "I cannot find that information in the provided content."

Context:
${pageContent}

Question: ${userQuestion}

Answer based strictly on the context provided above:`

    try {
      const response = await fetch(`${API_URL}?key=${API_KEY}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          contents: [{
            parts: [{
              text: prompt
            }]
          }]
        })
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const data = await response.json()
      
      if (data.candidates && data.candidates[0] && data.candidates[0].content) {
        return data.candidates[0].content.parts[0].text
      } else {
        throw new Error('Invalid response format from API')
      }
    } catch (error) {
      console.error('AI API Error:', error)
      if (error.message.includes('API key')) {
        return 'Please configure your API key in the ChatSidebar component.'
      } else if (error.message.includes('403') || error.message.includes('401')) {
        return 'Authentication failed. Please check your API key.'
      } else if (error.message.includes('429')) {
        return 'Rate limit exceeded. Please try again in a moment.'
      } else {
        return 'I\'m having trouble connecting right now. Please try again.'
      }
    }
  }

  const clearChat = () => {
    setMessages([{
      text: 'Chat cleared. Start a new conversation!',
      sender: 'ai',
      timestamp: new Date()
    }])
  }

  const handleSendMessage = async () => {
    if (!inputValue.trim() || isLoading) return

    const userMessage = { text: inputValue, sender: 'user', timestamp: new Date() }
    setMessages(prev => [...prev, userMessage])
    setInputValue('')
    setIsLoading(true)

    // Add loading message
    const loadingMessage = { text: 'Thinking...', sender: 'ai', timestamp: new Date(), isLoading: true }
    setMessages(prev => [...prev, loadingMessage])

    try {
      const pageContent = extractPageContent()
      const aiResponse = await generateAIResponse(inputValue, pageContent)
      
      // Replace loading message with actual response
      setMessages(prev => 
        prev.map(msg => 
          msg.isLoading 
            ? { text: aiResponse, sender: 'ai', timestamp: new Date() }
            : msg
        )
      )
    } catch (error) {
      console.error('Error generating response:', error)
      // Replace loading message with error message
      setMessages(prev => 
        prev.map(msg => 
          msg.isLoading 
            ? { text: 'I\'m having trouble connecting right now. Please try again.', sender: 'ai', timestamp: new Date() }
            : msg
        )
      )
    } finally {
      setIsLoading(false)
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  if (!isOpen) return null

  return (
    <div className="chat-sidebar-overlay">
      <div className="chat-sidebar">
        <div className="chat-header">
          <h3>Page Assistant</h3>
          <div className="chat-actions">
            <button className="clear-chat-button" onClick={clearChat} title="Clear chat">
              Clear
            </button>
          </div>
          <button className="close-button" onClick={onClose} aria-label="Close chat">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
            </svg>
          </button>
        </div>
        
        <div className="chat-messages">
          {messages.length === 0 ? (
            <div className="welcome-message">
              <p>Hi! I can help you with questions about this page. What would you like to know?</p>
            </div>
          ) : (
            messages.map((message, index) => (
              <div key={index} className={`message ${message.sender}`}>
                <div className="message-content">
                  {message.isLoading ? (
                    <div className="loading-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  ) : (
                    <p>{message.text}</p>
                  )}
                </div>
                <div className="message-time">
                  {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </div>
              </div>
            ))
          )}
          <div ref={messagesEndRef} />
        </div>
        
        <div className="chat-input">
          <textarea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Ask about this page..."
            rows={1}
            disabled={isLoading}
          />
          <button 
            onClick={handleSendMessage} 
            disabled={!inputValue.trim() || isLoading}
            className="send-button"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M22 2L11 13M22 2L15 22L11 13L2 9L22 2Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  )
}

export default ChatSidebar
