import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import MarketData from './MarketData.jsx'


createRoot(document.getElementById('root')).render(
  <StrictMode>
    <MarketData />
  </StrictMode>,
)
