import { useState } from 'react'
import axios from 'axios'

function App() {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [appStructure, setAppStructure] = useState(null)
  const [kotlinCode, setKotlinCode] = useState('')
  const [swiftCode, setSwiftCode] = useState('')
  const [buildStatus, setBuildStatus] = useState(null)
  const [buildProgress, setBuildProgress] = useState(0)

  const API_BASE = '/api'

  const sendMessage = async () => {
    if (!input.trim()) return

    const userMessage = { role: 'user', content: input }
    setMessages(prev => [...prev, userMessage])
    setLoading(true)

    try {
      const response = await axios.post(`${API_BASE}/chat`, {
        message: input
      })

      if (response.data.success) {
        setAppStructure(response.data.appStructure)
        setKotlinCode(response.data.kotlinCode)
        setSwiftCode(response.data.swiftCode)
        
        const aiMessage = { 
          role: 'assistant', 
          content: `✅ Aplikasi "${response.data.appStructure.nama}" berhasil dibuat!\n\n📱 Platform: ${response.data.appStructure.platform}\n🔧 Screens: ${response.data.appStructure.screens.length}\n💰 Monetization: ${response.data.appStructure.monetization.enabled ? 'Enabled' : 'Disabled'}\n🗄️ Database: ${response.data.appStructure.database.provider}`
        }
        setMessages(prev => [...prev, aiMessage])
      }
    } catch (error) {
      const errorMessage = { 
        role: 'assistant', 
        content: `❌ Error: ${error.response?.data?.error || error.message}`
      }
      setMessages(prev => [...prev, errorMessage])
    } finally {
      setLoading(false)
      setInput('')
    }
  }

  const handleBuild = async () => {
    if (!appStructure) return

    try {
      setBuildStatus('building')
      setBuildProgress(10)

      const response = await axios.post(`${API_BASE}/build`, {
        appDefinition: appStructure,
        platform: 'android'
      })

      if (response.data.success) {
        const buildId = response.data.buildId
        pollBuildStatus(buildId)
      }
    } catch (error) {
      setBuildStatus('failed')
      alert(`Build failed: ${error.message}`)
    }
  }

  const pollBuildStatus = async (buildId) => {
    const interval = setInterval(async () => {
      try {
        const response = await axios.get(`${API_BASE}/status/${buildId}`)
        
        if (response.data.success) {
          setBuildProgress(response.data.progress)
          setBuildStatus(response.data.status)

          if (response.data.status === 'completed') {
            clearInterval(interval)
            alert('✅ Build completed! APK ready for download.')
          } else if (response.data.status === 'failed') {
            clearInterval(interval)
            alert('❌ Build failed: ' + response.data.message)
          }
        }
      } catch (error) {
        clearInterval(interval)
        setBuildStatus('failed')
      }
    }, 2000)
  }

  const renderComponentPreview = (komponen) => {
    switch (komponen.tipe) {
      case 'TEXT':
        return <p key={komponen.id} className="text-gray-800">{komponen.label}</p>
      case 'BUTTON':
        return (
          <button 
            key={komponen.id}
            className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
          >
            {komponen.label || 'Button'}
          </button>
        )
      case 'TEXTFIELD':
        return (
          <input
            key={komponen.id}
            type="text"
            placeholder={komponen.label}
            className="border border-gray-300 rounded px-3 py-2 w-full"
          />
        )
      case 'SPACER':
        return <div key={komponen.id} className="h-4"></div>
      default:
        return (
          <div key={komponen.id} className="text-gray-400 text-sm">
            [{komponen.tipe}]
          </div>
        )
    }
  }

  const renderScreenPreview = (screen) => {
    return (
      <div key={screen.nama} className="border rounded-lg p-4 mb-4 bg-white shadow">
        <h3 className="font-bold text-lg mb-2">{screen.nama}</h3>
        <span className="text-xs bg-gray-200 px-2 py-1 rounded">{screen.tipe}</span>
        <div className="mt-4 space-y-2">
          {screen.komponen?.map(renderComponentPreview)}
        </div>
      </div>
    )
  }

  return (
    <div className="h-screen flex flex-col bg-gray-100">
      {/* Header */}
      <header className="bg-blue-600 text-white p-4 shadow">
        <h1 className="text-2xl font-bold">GenApp - Text to App Platform</h1>
        <p className="text-blue-100 text-sm">Buat aplikasi Android & iOS hanya dengan chat</p>
      </header>

      {/* Main Content */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Panel - Chat */}
        <div className="w-1/3 flex flex-col border-r bg-white">
          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.map((msg, idx) => (
              <div
                key={idx}
                className={`p-3 rounded-lg ${
                  msg.role === 'user' 
                    ? 'bg-blue-500 text-white ml-auto max-w-[80%]' 
                    : 'bg-gray-200 text-gray-800 mr-auto max-w-[80%]'
                }`}
              >
                <pre className="whitespace-pre-wrap text-sm">{msg.content}</pre>
              </div>
            ))}
            {loading && (
              <div className="bg-gray-200 p-3 rounded-lg inline-block">
                <div className="flex space-x-2">
                  <div className="w-2 h-2 bg-gray-500 rounded-full animate-bounce"></div>
                  <div className="w-2 h-2 bg-gray-500 rounded-full animate-bounce" style={{animationDelay: '0.1s'}}></div>
                  <div className="w-2 h-2 bg-gray-500 rounded-full animate-bounce" style={{animationDelay: '0.2s'}}></div>
                </div>
              </div>
            )}
          </div>

          {/* Input */}
          <div className="p-4 border-t">
            <div className="flex space-x-2">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                placeholder="Contoh: Buat aplikasi toko online dengan iklan banner..."
                className="flex-1 border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                disabled={loading}
              />
              <button
                onClick={sendMessage}
                disabled={loading || !input.trim()}
                className="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 disabled:bg-gray-400"
              >
                Send
              </button>
            </div>
          </div>
        </div>

        {/* Right Panel - Preview & Code */}
        <div className="w-2/3 flex flex-col overflow-hidden">
          {/* Tabs */}
          <div className="flex border-b bg-white">
            <button className="px-4 py-2 border-b-2 border-blue-500 text-blue-500 font-medium">
              Preview
            </button>
            <button className="px-4 py-2 text-gray-500 hover:text-gray-700">
              Kotlin Code
            </button>
            <button className="px-4 py-2 text-gray-500 hover:text-gray-700">
              Swift Code
            </button>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto p-4">
            {appStructure ? (
              <div>
                {/* App Info */}
                <div className="bg-blue-50 p-4 rounded-lg mb-4">
                  <h2 className="text-xl font-bold text-blue-800">{appStructure.nama}</h2>
                  <p className="text-blue-600 text-sm">{appStructure.description}</p>
                  <div className="flex gap-4 mt-2 text-xs text-blue-500">
                    <span>📦 {appStructure.packageName}</span>
                    <span>📱 {appStructure.platform}</span>
                    <span>🏷️ v{appStructure.version}</span>
                  </div>
                </div>

                {/* Build Button */}
                <div className="mb-4">
                  <button
                    onClick={handleBuild}
                    className="bg-green-500 text-white px-6 py-3 rounded-lg hover:bg-green-600 font-medium flex items-center gap-2"
                  >
                    <span>🚀</span> Build Android APK
                  </button>
                  
                  {buildStatus && (
                    <div className="mt-2">
                      <div className="flex justify-between text-sm text-gray-600 mb-1">
                        <span>Status: {buildStatus}</span>
                        <span>{buildProgress}%</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="bg-green-500 h-2 rounded-full transition-all duration-300"
                          style={{ width: `${buildProgress}%` }}
                        ></div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Screens Preview */}
                <h3 className="font-bold text-lg mb-2">Screens ({appStructure.screens.length})</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {appStructure.screens.map(renderScreenPreview)}
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-center h-full text-gray-400">
                <div className="text-center">
                  <p className="text-6xl mb-4">💬</p>
                  <p className="text-lg">Mulai chat untuk membuat aplikasi</p>
                  <p className="text-sm mt-2">Contoh: "Buat aplikasi e-commerce dengan fitur login dan keranjang belanja"</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default App
