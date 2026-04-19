"use client"

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Sidebar from '@/components/feed/Sidebar'
import MobileBottomNav from '@/components/feed/MobileBottomNav'
import SignalCard from '@/components/feed/SignalCard'
import { Plus, Search, Loader2, WifiOff, RefreshCw, X } from 'lucide-react'
import RaiseSignalModal from '@/components/feed/RaiseSignalModal'
import { useSignalStore, Signal, getSignalExpiration } from '@/store/useSignalStore'
import { useAuthStore } from '@/store/useAuthStore'
import { useNetworkStore } from '@/store/useNetworkStore'
import { useSearchStore } from '@/store/useSearchStore'
import { signalsApi, ApiSignal } from '@/lib/apiClient'
import SearchResultsPanel from '@/components/feed/SearchResultsPanel'

function formatInstagramTime(createdAt?: string) {
    const createdMs = createdAt ? new Date(createdAt).getTime() : 0;
    if (!createdMs) return 'now';
    const diffMs = Math.max(0, Date.now() - createdMs);
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHrs = Math.floor(diffMin / 60);
    const diffDays = Math.floor(diffHrs / 24);
    const diffWeeks = Math.floor(diffDays / 7);

    if (diffWeeks > 0) return `${diffWeeks}w`;
    if (diffDays > 0) return `${diffDays}d`;
    if (diffHrs > 0) return `${diffHrs}h`;
    if (diffMin > 0) return `${diffMin}m`;
    return 'now';
}

// Map backend ApiSignal → the shape SignalCard expects
function mapApiSignalToCard(s: ApiSignal) {
    const timeAgo = formatInstagramTime(s.createdAt);

    return {
        id: s.id,
        title: s.title || '(Untitled)',
        username: s.username || 'unknown',
        timeAgo,
        category: s.category || 'General',
        description: s.description || '',
        strength: s.signalStrength || '7 Days',
        stats: {
            responses: s.responseCount ?? 0,
            offers: s.offerCount ?? 0,
            views: s.viewCount ?? 0,
        },
        userPlan: s.userPlan || 'free',
        createdAt: s.createdAt,
        signalStrength: s.signalStrength
    }
}

export default function HomeFeed() {
    const router = useRouter()
    const { isAuthenticated, user } = useAuthStore()
    const { signals: localSignals } = useSignalStore()
    const { connections, sentRequests, pendingRequests, sendRequest } = useNetworkStore()
    const { query, setQuery, performSearch, clearSearch } = useSearchStore()

    // ── Backend signal state ──────────────────────────────────────────────────
    const [apiSignals, setApiSignals] = useState<ApiSignal[]>([])
    const [loading, setLoading] = useState(true)
    const [backendError, setBackendError] = useState<string | null>(null)
    const [isRaiseModalOpen, setIsRaiseModalOpen] = useState(false)
    const [refreshKey, setRefreshKey] = useState(0)

    // Debounced search
    useEffect(() => {
        const timer = setTimeout(() => {
            if (query.length >= 2) {
                performSearch(query)
            }
        }, 300)
        return () => clearTimeout(timer)
    }, [query, performSearch])

    useEffect(() => {
        let cancelled = false
        setLoading(true)
        setBackendError(null)

        signalsApi.getAll().then(({ data, error }) => {
            if (cancelled) return
            if (error || data === null) {
                setBackendError(error || 'Could not reach backend')
            } else {
                setApiSignals(data)
            }
            setLoading(false)
        })

        return () => { cancelled = true }
    }, [refreshKey])

    const requireAuth = (action: () => void) => {
        if (!isAuthenticated) {
            router.push('/auth')
        } else {
            action()
        }
    }

    // Merge backend signals + any local-only signals (created while offline)
    const backendIds = new Set(apiSignals.map(s => s.id))
    const localOnly = localSignals.filter(s => !backendIds.has(s.id))

    const displaySignals = [
        ...apiSignals.map(mapApiSignalToCard),
        ...localOnly.map(s => ({
            id: s.id,
            title: s.title,
            username: s.username,
            timeAgo: s.timeAgo,
            category: s.category,
            description: s.description,
            strength: s.strength,
            stats: s.stats,
            // Use the plan stored on the signal; if it's the current user's signal, use their live plan
            userPlan: s.username === user?.username
                ? (user?.subscription || user?.plan || s.userPlan || 'Free')
                : (s.userPlan || 'Free'),
            createdAt: s.createdAt
        })),
    ].filter(s => {
        const { isExpired } = getSignalExpiration(s);
        return !isExpired;
    })

    return (
        <div className="min-h-screen bg-background flex justify-center">
            <div className="max-w-[1400px] w-full flex flex-col md:flex-row mb-16 md:mb-0">
                <Sidebar />

                {/* Signals Feed */}
                <main className="flex-1 max-w-2xl w-full px-4 py-8 md:overflow-y-auto border-r border-border">
                    <header className="mb-8 flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center bg-background/90 backdrop-blur-md sticky top-0 z-20 py-4 -mx-4 px-4 border-b border-border shadow-sm">
                        <div className="flex items-center gap-4">
                            <h1 className="text-2xl font-bold font-display tracking-tight text-black">Signals Feed</h1>
                            
                            {/* Backend status and local mode notice moved into header for compactness */}
                            {!loading && backendError && (
                                <span className="flex items-center gap-1 text-[9px] font-extrabold uppercase tracking-tighter text-orange-600 border border-orange-200 bg-orange-50/80 px-2 py-0.5 rounded-full whitespace-nowrap">
                                    <WifiOff className="w-2.5 h-2.5" /> Local
                                </span>
                            )}
                        </div>
                        
                        <div className="flex gap-2 items-center w-full sm:flex-1 justify-end sm:max-w-md sm:ml-4">
                            <div className="relative group flex-1">
                                <div className="relative">
                                    <Search className={`absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 transition-colors ${query ? 'text-primary' : 'text-text-muted'}`} />
                                    <input 
                                        type="text"
                                        placeholder="Search signals..."
                                        value={query}
                                        onChange={(e) => setQuery(e.target.value)}
                                        className="pl-9 pr-4 py-2 bg-white/50 border border-border rounded-full text-sm focus:outline-none focus:border-primary/40 focus:ring-4 focus:ring-primary/5 w-full transition-all"
                                    />
                                    {query && (
                                        <button 
                                            onClick={clearSearch}
                                            className="absolute right-2 top-1/2 -translate-y-1/2 p-1 hover:bg-border/50 rounded-full"
                                        >
                                            <X className="w-3 h-3 text-text-muted" />
                                        </button>
                                    )}
                                </div>
                                <SearchResultsPanel />
                            </div>
                            <button
                                onClick={() => setRefreshKey(k => k + 1)}
                                title="Refresh"
                                className="p-2 border border-border rounded-full hover:bg-surface-2 transition-all shrink-0 bg-white"
                            >
                                <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
                            </button>
                            <button
                                onClick={() => requireAuth(() => setIsRaiseModalOpen(true))}
                                className="bg-black text-white px-4 py-2 rounded-full flex items-center gap-2 hover:bg-black/90 transition-all shrink-0 shadow-sm"
                            >
                                <Plus className="w-4 h-4" />
                                <span className="text-xs font-bold uppercase tracking-widest hidden sm:inline">Raise</span>
                            </button>
                        </div>
                    </header>

                    {/* Backend error notice */}
                    {backendError && (
                        <div className="mb-4 p-3 bg-orange-50 border border-orange-200 rounded-xl text-xs text-orange-700 flex items-start gap-2">
                            <WifiOff className="w-4 h-4 shrink-0 mt-0.5" />
                            <span>
                                <strong>Backend unreachable:</strong> {backendError}. Showing local signals only.
                                Make sure the Spring Boot server is running at <code className="font-mono bg-orange-100 px-1 rounded">localhost:8081</code>.
                            </span>
                        </div>
                    )}

                    {/* Signal list */}
                    {loading ? (
                        <div className="flex flex-col items-center justify-center py-24 gap-3 text-text-muted">
                            <Loader2 className="w-8 h-8 animate-spin" />
                            <p className="text-sm">Loading signals from backend…</p>
                        </div>
                    ) : displaySignals.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-24 gap-3 text-text-muted">
                            <Search className="w-10 h-10 opacity-30" />
                            <p className="text-sm">No signals yet. Be the first to raise one!</p>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {displaySignals.map(signal => (
                                <SignalCard
                                    key={signal.id}
                                    id={signal.id}
                                    title={signal.title}
                                    username={signal.username}
                                    timeAgo={signal.timeAgo}
                                    category={signal.category}
                                    description={signal.description}
                                    strength={signal.strength || (signal as any).signalStrength}
                                    stats={signal.stats}
                                    hideViews={true}
                                    userPlan={signal.userPlan}
                                    createdAt={signal.createdAt}
                                    onRefresh={() => setRefreshKey(k => k + 1)}
                                />
                            ))}
                        </div>
                    )}
                </main>

                <aside className="hidden lg:block w-[320px] p-8 space-y-4">
                    <div className="bg-white/[0.02] border border-border p-6 rounded-xl relative overflow-hidden group">
                        <div className="absolute inset-0 bg-gradient-to-br from-primary/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
                        <h3 className="font-display text-lg mb-2 relative z-10">Need Market Analysis?</h3>
                        <p className="text-text-secondary text-sm mb-6 relative z-10">Real World Data. No Hallucinations. Powered by Starto AI.</p>
                        <button onClick={() => requireAuth(() => router.push('/explore'))} className="w-full bg-primary text-white py-3 rounded-md font-medium text-sm flex items-center justify-center gap-2 hover:opacity-90 relative z-10">
                            Launch Explore →
                        </button>
                    </div>

                    <div
                        onClick={() => router.push('/subscription')}
                        className="bg-white border border-border p-6 rounded-xl cursor-pointer hover:border-primary hover:shadow-md transition-all group relative overflow-hidden"
                    >
                        <div className="absolute inset-0 bg-gradient-to-br from-yellow-50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
                        <p className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-2 relative z-10">Sponsored</p>
                        <h3 className="font-display text-base mb-1 relative z-10">Want to run ads here?</h3>
                        <p className="text-text-secondary text-xs mb-4 relative z-10 leading-relaxed">Reach 1000s of founders, investors &amp; mentors in our ecosystem.</p>
                        <button
                            onClick={() => router.push('/subscription')}
                            className="w-full bg-black text-white py-3 rounded-md font-medium text-sm flex items-center justify-center gap-2 hover:opacity-80 relative z-10 transition-opacity"
                        >
                            Get started →
                        </button>
                    </div>
                </aside>
                
                <RaiseSignalModal 
                    isOpen={isRaiseModalOpen} 
                    onClose={() => {
                        setIsRaiseModalOpen(false)
                        // Refresh feed after signal creation attempt
                        setRefreshKey(k => k + 1)
                    }} 
                />
                <MobileBottomNav />
            </div>
        </div>
    )
}
