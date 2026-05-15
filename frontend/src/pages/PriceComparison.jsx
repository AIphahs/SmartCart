import { useState } from 'react'
import { api } from '../api/client'

export default function PriceComparison() {
  const [product, setProduct] = useState('')
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState('')

  const search = async e => {
    e.preventDefault()
    if (!product.trim()) return
    setLoading(true)
    setSearched(product.trim())
    try {
      const res = await api.get(`/analytics/prices/compare?product=${encodeURIComponent(product.trim())}`)
      setResults(res.data)
    } catch {
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Comparaison des prix</h1>
        <p className="text-gray-500 text-sm mt-1">Compare le prix d'un produit entre différents magasins.</p>
      </div>

      <form onSubmit={search} className="flex gap-3">
        <input
          type="text"
          value={product}
          onChange={e => setProduct(e.target.value)}
          placeholder="Ex: lait, beurre, eau..."
          className="flex-1 border rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
        />
        <button
          type="submit"
          disabled={loading || !product.trim()}
          className="bg-emerald-600 text-white px-6 py-2 rounded-lg text-sm font-medium hover:bg-emerald-700 disabled:opacity-50"
        >
          {loading ? 'Recherche...' : 'Comparer'}
        </button>
      </form>

      {results !== null && (
        results.length === 0 ? (
          <div className="text-center py-14 text-gray-400">
            <p className="text-4xl mb-3">🔍</p>
            <p>Aucun résultat pour « {searched} »</p>
            <p className="text-xs mt-1">Scannez d'abord des reçus contenant ce produit</p>
          </div>
        ) : (
          <div className="bg-white rounded-xl shadow overflow-hidden">
            <div className="px-6 py-4 border-b flex items-center justify-between">
              <p className="font-semibold text-gray-700">Résultats pour « {searched} »</p>
              <span className="text-xs text-gray-400">{results.length} magasin{results.length > 1 ? 's' : ''}</span>
            </div>
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
                <tr>
                  <th className="px-6 py-3 text-left">Magasin</th>
                  <th className="px-6 py-3 text-right">Moy.</th>
                  <th className="px-6 py-3 text-right">Min</th>
                  <th className="px-6 py-3 text-right">Max</th>
                  <th className="px-6 py-3 text-right">Fois</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {results.map((r, i) => (
                  <tr key={i} className={`hover:bg-gray-50 ${i === 0 ? 'font-medium' : ''}`}>
                    <td className="px-6 py-3 flex items-center gap-2">
                      {i === 0 && <span className="text-emerald-500 text-xs">✓ moins cher</span>}
                      {r.storeName}
                    </td>
                    <td className="px-6 py-3 text-right font-semibold">
                      {r.averagePrice != null ? `${Number(r.averagePrice).toFixed(2)} €` : '—'}
                    </td>
                    <td className="px-6 py-3 text-right text-emerald-600">
                      {r.minPrice != null ? `${Number(r.minPrice).toFixed(2)} €` : '—'}
                    </td>
                    <td className="px-6 py-3 text-right text-red-400">
                      {r.maxPrice != null ? `${Number(r.maxPrice).toFixed(2)} €` : '—'}
                    </td>
                    <td className="px-6 py-3 text-right text-gray-400">{r.occurrences}×</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
    </div>
  )
}
