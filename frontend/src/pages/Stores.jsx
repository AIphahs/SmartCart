import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import Spinner from '../components/Spinner'

export default function Stores() {
  const [stores, setStores] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/stores').then(res => setStores(res.data)).finally(() => setLoading(false))
  }, [])

  if (loading) return <Spinner />

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">Magasins</h1>

      {stores.length === 0 ? (
        <div className="text-center py-20 text-gray-400">
          <p className="text-5xl mb-4">🏪</p>
          <p className="text-lg">Aucun magasin enregistré</p>
          <p className="text-sm mt-1">Les magasins apparaissent automatiquement après avoir scanné des reçus</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {stores.map(s => (
            <div key={s.id} className="bg-white rounded-xl shadow p-5 flex items-center justify-between">
              <div>
                <p className="font-semibold text-gray-800">{s.name}</p>
                {s.address && <p className="text-sm text-gray-400 mt-0.5">{s.address}</p>}
                <p className="text-sm text-gray-500 mt-1.5">
                  {s.receiptCount} reçu{s.receiptCount !== 1 ? 's' : ''}
                </p>
              </div>
              <Link
                to={`/receipts`}
                className="text-emerald-500 text-sm hover:underline shrink-0"
              >
                Voir →
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
