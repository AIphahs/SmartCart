import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Dashboard from './pages/Dashboard'
import Receipts from './pages/Receipts'
import ReceiptDetail from './pages/ReceiptDetail'
import PriceComparison from './pages/PriceComparison'
import Stores from './pages/Stores'

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex min-h-screen bg-gray-50">
        <Sidebar />
        <main className="flex-1 p-6 overflow-auto">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/receipts" element={<Receipts />} />
            <Route path="/receipts/:id" element={<ReceiptDetail />} />
            <Route path="/compare" element={<PriceComparison />} />
            <Route path="/stores" element={<Stores />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
