import { useState } from 'react'
import './App.css'
import ChatButton from './components/ChatButton'
import ChatSidebar from './components/ChatSidebar'

function App() {
  const [isChatOpen, setIsChatOpen] = useState(false)

  return (
    <div className="app">
      <header className="app-header">
        <h1>Literacy Rate in India</h1>
        <p>Exploring Educational Progress, Challenges, and Transformative Role of Artificial Intelligence</p>
      </header>

      <main id="mainContent" className="main-content">
        <section className="section">
          <h2>Educational Growth and Digital Transformation in India</h2>
          <p className="section-intro">
            Over the past several decades, India has made significant progress in improving literacy levels across the country. Government reforms, technological advancements, and digital education platforms have played an important role in enhancing access to education. Despite improvements, regional and gender-based disparities continue to influence literacy outcomes.
          </p>

          <h3>Current Literacy Statistics</h3>
          <p>
            According to recent national data, India's overall literacy rate stands at approximately 77.7%. 
            Male literacy is around 84.7%, while female literacy is approximately 70.3%. 
            Kerala leads the nation with a literacy rate exceeding 96%, followed by states such as Delhi and Himachal Pradesh. 
            However, certain states continue to report literacy rates below 70%, highlighting uneven development.
          </p>

          <h3>Historical Progress in Literacy</h3>
          <p>
            At the time of independence in 1947, India's literacy rate was just 18.3%. 
            By 1991, it had increased to 52.2%, and it crossed 74% in the 2011 Census. 
            Continuous investments in primary education, mid-day meal schemes, and universal enrollment initiatives have significantly improved literacy across generations.
          </p>

          <h3>Urban vs Rural Literacy Gap</h3>
          <p>
            Urban regions generally report literacy rates above 85%, whereas rural areas average around 73%. 
            Challenges such as limited infrastructure, shortage of qualified teachers, and economic constraints contribute to lower literacy levels in rural districts. 
            Bridging this gap remains a key focus of national education policies.
          </p>

          <h3>Gender Disparities in Education</h3>
          <p>
            Although the gender gap in literacy has narrowed over years, disparities still exist. 
            Female literacy has improved significantly due to awareness campaigns and scholarship programs. 
            Government initiatives such as "Beti Bachao, Beti Padhao" have contributed to higher female school enrollment and improved literacy outcomes.
          </p>

          <h3>Role of Government Policies</h3>
          <p>
            Programs such as Right to Education Act (RTE), Digital India, and National Education Policy (NEP) 2020 
            aim to strengthen foundational literacy and numeracy. These initiatives focus on improving curriculum quality, 
            teacher training, and digital accessibility to ensure inclusive education.
          </p>

          <h3>Impact of Artificial Intelligence on Education</h3>
          <p>
            Artificial Intelligence is rapidly transforming India's education system. 
            AI-powered platforms provide personalized learning experiences tailored to individual student needs. 
            Adaptive assessments analyze student performance and recommend targeted improvements, 
            increasing overall learning efficiency.
          </p>

          <h3>AI-Based Personalized Learning</h3>
          <p>
            Machine learning algorithms analyze student behavior and performance data to create customized study plans. 
            These systems identify weak areas and provide instant feedback. 
            AI tutors can operate 24/7, allowing students to learn at their own pace and improve literacy skills effectively.
          </p>

          <h3>AI in Rural and Remote Education</h3>
          <p>
            AI-driven mobile applications and smart classrooms are extending quality education to remote villages. 
            Voice-based AI tools support regional languages, helping students understand lessons in their native language. 
            Digital content delivery reduces dependency on physical textbooks and improves access to updated resources.
          </p>

          <h3>Challenges in Implementing AI in Education</h3>
          <p>
            Despite its potential, implementing AI in education faces challenges such as limited internet connectivity, 
            data privacy concerns, and insufficient digital infrastructure in rural areas. 
            Ensuring equitable access to AI technologies remains a critical concern for policymakers.
          </p>

          <h3>Future Outlook for Literacy in India</h3>
          <p>
            With continuous technological integration and government reforms, India aims to push its literacy rate beyond 85% 
            in the coming decade. AI-based educational solutions, digital classrooms, and policy-driven initiatives 
            are expected to reduce literacy disparities and improve overall educational quality nationwide.
          </p>
        </section>
      </main>

      <div className="app-actions">
        <ChatButton onClick={() => setIsChatOpen(true)} />
        <button className="annotation-button" title="Annotation">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" fill="none"/>
            <path d="M9 12l3 3h3v-6H9z" fill="currentColor"/>
          </svg>
        </button>
      </div>
      <ChatSidebar 
        isOpen={isChatOpen} 
        onClose={() => setIsChatOpen(false)} 
      />
    </div>
  )
}

export default App
