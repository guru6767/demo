"use client"

import Sidebar from '@/components/feed/Sidebar'
import { Plus, Search, Edit3, Trash2, ArrowUpRight, Loader2, RefreshCw } from 'lucide-react'
import { useSignalStore, getSignalExpiration } from '@/store/useSignalStore'
import { useAuthStore } from '@/store/useAuthStore'
import { signalsApi, ApiSignal } from '@/lib/apiClient'
import RaiseSignalModal from '@/components/feed/RaiseSignalModal'
import InsightsModal from '@/components/feed/InsightsModal'
import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'

export default function MySignals() {
    const router = useRouter()
    const { signals: localSignals, deleteSignal: deleteLocalSignal } = useSignalStore()
    const { user, isAuthenticated, token } = useAuthStore()
    const [isRaiseModalOpen, setIsRaiseModalOpen] = useState(false)
    const [editingSignal, setEditingSignal] = useState<any>(null)
    const [viewingInsightsSignal, setViewingInsightsSignal] = useState<any>(null)
    const [activeFilter, setActiveFilter] = useState<string | null>(null)
    const [apiSignals, setApiSignals] = useState<ApiSignal[]>([])
    const [loading, setLoading] = useState(true)
    const [deletingId, setDeletingId] = useState<string | null>(null)
    const [refreshKey, setRefreshKey] = useState(0)

    useEffect(() => {
        if (!isAuthenticated) router.push('/auth')
    }, [isAuthenticated, router])

    const fetchMySignals = useCallback(async () => {
        if (!user?.username || !token) return
        setLoading(true)
        const authToken = token.startsWith('local-') ? token.replace('local-', 'dev_') : token
        const { data } = await signalsApi.getMine(authToken)
        if (data) setApiSignals(data)
        setLoading(false)
    }, [user?.username, token, refreshKey])

    useEffect(() => { fetchMySignals() }, [fetchMySignals])

    const categories = ['Talent', 'Founder', 'Mentor', 'Instant Help']

    // Backend signals for this user
    const backendIds = new Set(apiSignals.map(s => s.id))

    // Local-only signals (not yet in backend) for this user
    const localOnly = localSignals.filter(s =>
        s.username === user?.username && !backendIds.has(s.id)
    )

    // Merge and map to unified display shape
    const allMySignals = [
        ...apiSignals.map(s => ({
            id: s.id,
            title: s.title,
            category: s.category,
            description: s.description,
            status: s.status === 'open' ? 'Active' : 'Closed',
            stats: { responses: s.responseCount ?? 0, offers: s.offerCount ?? 0, views: s.viewCount ?? 0 },
            createdAt: s.createdAt,
            strength: s.signalStrength || '7 Days',
            source: 'backend' as const
        })),
        ...localOnly.map(s => ({
            id: s.id,
            title: s.title,
            category: s.category,
            description: s.description,
            status: s.status,
            stats: s.stats,
            createdAt: s.createdAt ? String(s.createdAt) : undefined,
            strength: s.strength,
            source: 'local' as const
        }))
    ].filter(s => {
        const { isExpired } = getSignalExpiration(s)
        return !isExpired && s.status === 'Active' && (activeFilter ? s.category === activeFilter : true)
    })

    const handleDelete = async (id: string, source: 'backend' | 'local') => {
        if (!confirm('Delete this signal?')) return
        setDeletingId(id)
        if (source === 'backend') {
            const authToken = token?.startsWith('local-')
                ? token.replace('local-', 'dev_')
                : (token || (user?.username ? `dev_${user.username}` : ''))
            const { error } = await signalsApi.delete(id, authToken)
            if (!error) {
                setApiSignals(prev => prev.filter(s => s.id !== id))
            } else {
                console.error('Delete error:', error)
                alert('Could not delete from server: ' + error)
            }
        } else {
            deleteLocalSignal(id)
        }
        setDeletingId(null)
    }

    if (!isAuthenticated || !user) return <div className="min-h-screen bg-background flex justify-center items-center text-text-muted">Loading...</div>

    return (
        <div className="min-h-screen bg-background flex justify-center">
            <div className="max-w-[1400px] w-full flex">
                <Sidebar />

                <main className="flex-1 max-w-[680px] border-r border-border min-h-screen p-6">
                    <div className="flex justify-between items-center mb-8">
                        <div>
                            <h1 className="text-2xl font-display">My Signals</h1>
                            <p className="text-xs text-text-secondary mt-1">Manage and track your active requests.</p>
                        </div>
                        <div className="flex gap-2">
                            <button
                                onClick={() => setRefreshKey(k => k + 1)}
                                className="p-2 border border-border rounded-full hover:bg-surface-2 transition-all bg-white"
                                title="Refresh"
                            >
                                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
                            </button>
                            <button onClick={() => setIsRaiseModalOpen(true)} className="bg-primary text-white px-4 py-2 rounded-md flex items-center gap-2 hover:opacity-90">
                                <Plus className="w-5 h-5" /> New Signal
                            </button>
                        </div>
                    </div>

                    <div className="flex gap-2 mb-8 items-center flex-wrap">
                        <button
                            onClick={() => setActiveFilter(null)}
                            className={`px-4 py-2 border rounded-md text-sm font-medium transition-all ${!activeFilter ? 'bg-primary text-white border-primary' : 'bg-white border-border text-text-secondary hover:bg-surface-2'}`}
                        >
                            All
                        </button>
                        {categories.map(cat => (
                            <button
                                key={cat}
                                onClick={() => setActiveFilter(cat)}
                                className={`px-4 py-2 border rounded-md text-sm font-medium transition-all ${activeFilter === cat ? 'bg-primary text-white border-primary' : 'bg-white border-border text-text-secondary hover:bg-surface-2'}`}
                            >
                                {cat}
                            </button>
                        ))}
                    </div>

                    {loading ? (
                        <div className="flex justify-center items-center py-24 text-text-muted gap-3">
                            <Loader2 className="w-6 h-6 animate-spin" />
                            <span className="text-sm">Loading your signals…</span>
                        </div>
                    ) : allMySignals.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-24 gap-3 text-text-muted">
                            <p className="text-sm">No signals yet. Raise one!</p>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {allMySignals.map(signal => (
                                <div key={signal.id} className="bg-white border border-border p-5 rounded-xl transition-all hover:shadow-sm">
                                    <div className="flex justify-between items-start mb-4">
                                        <div className="flex items-center gap-2">
                                            <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full ${signal.status === 'Active' ? 'bg-accent-green/10 text-accent-green' : 'bg-accent-blue/10 text-accent-blue'}`}>
                                                {signal.status}
                                            </span>
                                            {signal.source === 'local' && (
                                                <span className="text-[9px] text-orange-500 border border-orange-200 px-1.5 py-0.5 rounded-full">Local</span>
                                            )}
                                        </div>
                                        <div className="flex gap-2">
                                            <button
                                                onClick={(e) => { e.stopPropagation(); setEditingSignal(signal as any); }}
                                                className="p-1.5 text-text-muted hover:text-primary transition-all"
                                                title="Edit"
                                            >
                                                <Edit3 className="w-4 h-4" />
                                            </button>
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleDelete(signal.id, signal.source); }}
                                                disabled={deletingId === signal.id}
                                                className="p-1.5 text-text-muted hover:text-red-500 transition-all disabled:opacity-40"
                                                title="Delete"
                                            >
                                                {deletingId === signal.id
                                                    ? <Loader2 className="w-4 h-4 animate-spin" />
                                                    : <Trash2 className="w-4 h-4" />
                                                }
                                            </button>
                                        </div>
                                    </div>
                                    <h3 className="font-medium text-lg mb-1">{signal.title}</h3>
                                    {signal.description && <p className="text-sm text-text-secondary mb-4 line-clamp-2">{signal.description}</p>}
                                    <div className="flex items-center justify-between">
                                        <div className="flex gap-6">
                                            <div>
                                                <p className="text-[10px] text-text-muted uppercase">Responses</p>
                                                <p className="font-mono text-sm">{signal.stats.responses}</p>
                                            </div>
                                            <div>
                                                <p className="text-[10px] text-text-muted uppercase">Offers</p>
                                                <p className="font-mono text-sm">{signal.stats.offers}</p>
                                            </div>
                                            <div>
                                                <p className="text-[10px] text-text-muted uppercase">Views</p>
                                                <p className="font-mono text-sm">{signal.stats.views}</p>
                                            </div>
                                        </div>
                                        <button onClick={() => setViewingInsightsSignal(signal)} className="text-primary text-sm font-medium flex items-center gap-1 hover:underline">
                                            View Detail <ArrowUpRight className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </main>

                <aside className="hidden lg:block w-[320px] p-8 space-y-6">
                    <div className="bg-primary text-white p-6 rounded-xl relative overflow-hidden">
                        <div className="relative z-10">
                            <h3 className="font-display text-lg mb-2">Signal Insights</h3>
                            <p className="text-white/60 text-xs mb-6">Your signals are outperforming 85% of users in Bangalore.</p>
                            <div className="space-y-4">
                                <div className="flex justify-between items-end">
                                    <span className="text-xs uppercase tracking-widest text-white/40">Efficiency</span>
                                    <span className="text-2xl font-mono">92%</span>
                                </div>
                                <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                                    <div className="bg-accent-green h-full w-[92%]" />
                                </div>
                            </div>
                        </div>
                        <div className="absolute -right-4 -bottom-4 w-24 h-24 bg-white/5 rounded-full blur-2xl" />
                    </div>

                    <div className="bg-white border border-border p-6 rounded-xl shadow-sm">
                        <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-text-muted mb-3">Sponsored</p>
                        <h3 className="font-display text-lg mb-2 leading-snug">Want to run ads here?</h3>
                        <p className="text-sm text-text-secondary leading-relaxed mb-5">Reach 1000s of founders, investors &amp; mentors in our ecosystem.</p>
                        <button
                            onClick={() => router.push('/subscription')}
                            className="w-full bg-black text-white py-3 rounded-xl font-bold uppercase tracking-widest text-[10px] hover:bg-primary transition-colors"
                        >
                            Get started →
                        </button>
                    </div>
                </aside>
            </div>

            <RaiseSignalModal
                isOpen={isRaiseModalOpen || !!editingSignal}
                onClose={() => {
                    setIsRaiseModalOpen(false)
                    setEditingSignal(null)
                    setRefreshKey(k => k + 1)
                }}
                editSignal={editingSignal}
            />
            {viewingInsightsSignal && (
                <InsightsModal
                    isOpen={!!viewingInsightsSignal}
                    onClose={() => setViewingInsightsSignal(null)}
                    stats={viewingInsightsSignal.stats}
                    signalTitle={viewingInsightsSignal.title}
                />
            )}
        </div>
    )
}
