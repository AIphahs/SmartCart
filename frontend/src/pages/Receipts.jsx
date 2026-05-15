import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import UploadModal from '../components/UploadModal'
import Spinner from '../components/Spinner'

export default function Receipts() {
  const [receipts, setReceipts] = useState([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const fetchReceipts = (p = 0) => {
    setLoading(true)
    api.get(`/receipts?page=${p}&size=10`)
      .then(res => {
        setReceipts(res.data.content)
        setTotalPages(res.data.totalPages)
        setPage(p)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchReceipts() }, [])

  const handleSuccess = () => {
    setShowModal(false)
    fetchReceipts(0)
  }

  const handleDelete = async id => {
    if (!window.confirm('Supprimer ce reçu ?')) return
    await api.delete(`/receipts/${id}`)
    fetchReceipts(page)
  }

  if (loading) return <Spinner />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">Reçus</h1>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 bg-emerald-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-emerald-700"
        >
          <span className="text-lg leading-none">+</span> Scanner un reçu
        </button>
      </div>

      {receipts.length === 0 ? (
        <div className="text-center py-20 text-gray-400">
          <p className="text-5xl mb-4">🧾</p>
          <p className="text-lg">Aucun reçu pour l'instant</p>
          <p className="text-sm mt-1">Clique sur "Scanner un reçu" pour commencer</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-500 uppercase text-xs">
              <tr>
                <th className="px-6 py-3 text-left">Magasin</th>
                <th className="px-6 py-3 text-left">Date</th>
                <th className="px-6 py-3 text-right">Total</th>
                <th className="px-6 py-3 text-center">Articles</th>
                <th className="px-6 py-3 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {receipts.map(r => (
                <tr key={r.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4 font-medium text-gray-800">{r.store?.name ?? '—'}</td>
                  <td className="px-6 py-4 text-gray-500">{r.purchaseDate ?? '—'}</td>
                  <td className="px-6 py-4 text-right font-semibold text-emerald-600">
                    {r.totalAmount != null ? `${Number(r.totalAmount).toFixed(2)} €` : '—'}
                  </td>
                  <td className="px-6 py-4 text-center text-gray-500">{r.items?.length ?? 0}</td>
                  <td className="px-6 py-4 text-center space-x-4">
                    <Link to={`/receipts/${r.id}`} className="text-blue-500 hover:underline">Voir</Link>
                    <button onClick={() => handleDelete(r.id)} className="text-red-400 hover:underline">Supprimer</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-3">
          <button
            disabled={page === 0}
            onClick={() => fetchReceipts(page - 1)}
            className="px-3 py-1 rounded border text-sm disabled:opacity-40 hover:bg-gray-50"
          >
            ← Précédent
          </button>
          <span className="text-sm text-gray-500">Page {page + 1} / {totalPages}</span>
          <button
            disabled={page === totalPages - 1}
            onClick={() => fetchReceipts(page + 1)}
            className="px-3 py-1 rounded border text-sm disabled:opacity-40 hover:bg-gray-50"
          >
            Suivant →
          </button>
        </div>
      )}

      {showModal && <UploadModal onClose={() => setShowModal(false)} onSuccess={handleSuccess} />}
    </div>
  )
}
