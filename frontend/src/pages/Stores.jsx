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
            <Link
              key={s.id}
              to={`/stores/${s.id}`}
              className="bg-white rounded-xl shadow p-5 flex items-center justify-between hover:shadow-md transition-shadow"
            >
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <p className="font-semibold text-gray-800 truncate">{s.name}</p>
                  {s.hours?.length > 0 && (
                    <span className={`px-1.5 py-0.5 rounded-full text-[10px] font-medium shrink-0 ${
                      s.openNow ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {s.openNow ? 'Ouvert' : 'Fermé'}
                    </span>
                  )}
                </div>
                {s.address && <p className="text-sm text-gray-400 mt-0.5 truncate">{s.address}</p>}
                <p className="text-sm text-gray-500 mt-1.5">
                  {s.receiptCount} reçu{s.receiptCount !== 1 ? 's' : ''}
                </p>
              </div>
              <span className="text-emerald-500 text-sm shrink-0">Voir →</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
