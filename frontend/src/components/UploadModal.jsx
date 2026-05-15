import { useState, useRef } from 'react'
import { api } from '../api/client'

const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf']

export default function UploadModal({ onClose, onSuccess }) {
  const [file, setFile] = useState(null)
  const [storeName, setStoreName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [dragOver, setDragOver] = useState(false)
  const fileRef = useRef()

  const handleFile = f => {
    if (!f) return
    if (!ACCEPTED.includes(f.type)) {
      setError('Format non supporté. Utilise JPG, PNG ou PDF.')
      return
    }
    setFile(f)
    setError('')
  }

  const handleSubmit = async e => {
    e.preventDefault()
    if (!file) { setError('Sélectionne un fichier'); return }

    setLoading(true)
    const formData = new FormData()
    formData.append('file', file)
    if (storeName.trim()) formData.append('storeName', storeName.trim())

    try {
      const res = await api.post('/receipts/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      onSuccess(res.data)
    } catch (err) {
      setError(err.response?.data?.detail || "Erreur lors du traitement du reçu.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold text-gray-800">Uploader un reçu</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div
            className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors ${
              dragOver ? 'border-emerald-500 bg-emerald-50' : 'border-gray-300 hover:border-emerald-400'
            }`}
            onClick={() => fileRef.current.click()}
            onDragOver={e => { e.preventDefault(); setDragOver(true) }}
            onDragLeave={() => setDragOver(false)}
            onDrop={e => { e.preventDefault(); setDragOver(false); handleFile(e.dataTransfer.files[0]) }}
          >
            <input
              ref={fileRef}
              type="file"
              accept="image/*,.pdf"
              className="hidden"
              onChange={e => handleFile(e.target.files[0])}
            />
            {file ? (
              <p className="text-emerald-600 font-medium">✅ {file.name}</p>
            ) : (
              <>
                <p className="text-4xl mb-2">📸</p>
                <p className="text-gray-500 text-sm">Glisse une image ici ou clique pour choisir</p>
                <p className="text-gray-400 text-xs mt-1">JPG, PNG, PDF — max 10 MB</p>
              </>
            )}
          </div>

          <div>
            <label className="block text-sm text-gray-600 mb-1">Nom du magasin (optionnel)</label>
            <input
              type="text"
              value={storeName}
              onChange={e => setStoreName(e.target.value)}
              placeholder="Ex: Carrefour, Lidl..."
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          {error && <p className="text-red-500 text-sm">{error}</p>}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border rounded-lg text-sm text-gray-600 hover:bg-gray-50"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading || !file}
              className="flex-1 px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 disabled:opacity-50"
            >
              {loading ? 'Traitement...' : 'Analyser le reçu'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
