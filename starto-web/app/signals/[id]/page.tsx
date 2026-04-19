"use client"

import { useState, useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { 
    ArrowLeft, 
    Zap, 
    MessageSquare, 
    Clock, 
    Eye, 
    UserPlus, 
    Share2, 
    MapPin, 
    CheckCheck,
    Loader2,
    Calendar,
    ChevronRight,
    Search
} from 'lucide-react'
import Image from 'next/image'
import Link from 'next/link'
import { motion, AnimatePresence } from 'framer-motion'
import Sidebar from '@/components/feed/Sidebar'
import MobileBottomNav from '@/components/feed/MobileBottomNav'
import VerifiedAvatar from '@/components/feed/VerifiedAvatar'
import { signalsApi, ApiSignal, usersApi, ApiUser } from '@/lib/apiClient'
import { useAuthStore } from '@/store/useAuthStore'
import { useSignalStore, getSignalExpiration } from '@/store/useSignalStore'
import { useNetworkStore } from '@/store/useNetworkStore'
import { useResponseStore } from '@/store/useResponseStore'

// Reuse the map function to convert ApiSignal to card-like shape if needed, 
// but here we want the raw data for maximum detail.

export default function SignalDetailPage() {
    const { id } = useParams()
    const router = useRouter()
    const { user: currentUser, token } = useAuthStore()
    const { signals: localSignals, addComment, addReply } = useSignalStore()
    const { sentRequests, pendingRequests, sendRequest, fetchRequests } = useNetworkStore()
    const { addResponse, hasResponded } = useResponseStore()

    const [signal, setSignal] = useState<ApiSignal | null>(null)
    const [owner, setOwner] = useState<ApiUser | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [commentText, setCommentText] = useState('')
    const [replyToId, setReplyToId] = useState<string | null>(null)
    const [replyText, setReplyText] = useState('')

    useEffect(() => {
        if (!id) return
        
        setLoading(true)
        
        // Simple UUID regex check to avoid 404 errors for local-only signals during prefetching
        const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id as string);
        
        if (!isUuid) {
            // Local-only signal (temporary ID) — fetch from store only
            const local = localSignals.find(s => s.id === id);
            if (local) {
                setSignal(local as any);
                setLoading(false);
            } else {
                setError('Local signal not found');
                setLoading(false);
            }
            return;
        }

        signalsApi.getById(id as string).then(({ data, error }) => {
            if (error || !data) {
                console.error('API Signal error:', error, 'ID:', id)
                // Fallback to local storage if backend fails
                const local = localSignals.find(s => s.id === id)
                if (local) {
                    setSignal(local as any)
                    setLoading(false)
                } else {
                    setError(error || 'Signal not found')
                    setLoading(false)
                }
            } else {
                setSignal(data)
                // Fetch owner details
                if (data.username) {
                    usersApi.getByUsername(data.username).then(({ data: userData }) => {
                        setOwner(userData)
                    })
                }
                setLoading(false)
            }
        })
    }, [id, localSignals])

    if (loading) {
        return (
            <div className="min-h-screen bg-background flex items-center justify-center">
                <div className="flex flex-col items-center gap-4">
                    <Loader2 className="w-8 h-8 text-primary animate-spin" />
                    <p className="text-sm font-medium text-text-muted">Loading signal details...</p>
                </div>
            </div>
        )
    }

    if (error || !signal) {
        return (
            <div className="min-h-screen bg-background flex items-center justify-center p-6">
                <div className="text-center">
                    <h2 className="text-2xl font-display mb-2">Signal not found</h2>
                    <p className="text-text-muted mb-6">This signal may have expired or was removed from the ecosystem.</p>
                    <button 
                        onClick={() => router.push('/feed')}
                        className="bg-primary text-white px-6 py-2 rounded-md font-medium"
                    >
                        Back to Feed
                    </button>
                </div>
            </div>
        )
    }

    const isOwner = currentUser?.username === signal.username
    const alreadyConnected = sentRequests.some(r => r.username === signal.username && r.status === 'accepted') || 
                             pendingRequests.some(r => r.username === signal.username && r.status === 'accepted')
    const alreadyPending = sentRequests.some(r => r.username === signal.username && r.status === 'pending')
    const alreadyResponded = hasResponded(signal.id)

    // Calculate time details
    const createdAt = signal.createdAt ? new Date(signal.createdAt) : new Date(Date.now() - (1000 * 60 * 60 * 24))
    const { isExpired, daysLeft, hoursLeft, totalDuration, progressPercent } = getSignalExpiration(signal)

    return (
        <div className="min-h-screen bg-background flex justify-center">
            <div className="max-w-[1400px] w-full flex flex-col md:flex-row pb-16 md:pb-0">
                <Sidebar />

                <main className="flex-1 w-full max-w-[900px] md:border-r border-border min-h-screen p-4 md:p-8">
                    {/* Top Navigation */}
                    <div className="flex items-center justify-between mb-8">
                        <button 
                            onClick={() => router.back()}
                            className="flex items-center gap-2 text-text-muted hover:text-black transition-colors group"
                        >
                            <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
                            <span className="text-sm font-medium uppercase tracking-widest">Back</span>
                        </button>
                        <div className="flex gap-3">
                            <button className="p-2 hover:bg-surface-2 rounded-full transition-colors">
                                <Share2 className="w-4 h-4 text-text-muted" />
                            </button>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                        {/* Main Content Area */}
                        <div className="lg:col-span-2 space-y-8">
                            {/* Header Section */}
                            <div>
                                <div className="flex items-center gap-2 mb-3">
                                    <span className="text-[10px] font-bold uppercase tracking-[0.2em] bg-primary text-white px-2 py-0.5 rounded-full">
                                        {signal.category || 'General'}
                                    </span>
                                    <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-text-muted border border-border px-2 py-0.5 rounded-full">
                                        {signal.type === 'need' ? '🚨 Looking for' : '💡 Offering'}
                                    </span>
                                </div>
                                <h1 className="text-4xl font-display leading-tight mb-4">{signal.title}</h1>
                                <div className="flex items-center gap-6 text-sm text-text-muted">
                                    <div className="flex items-center gap-1.5 font-medium">
                                        <Calendar className="w-4 h-4" />
                                        <span>Opened {new Date(createdAt).toLocaleDateString()}</span>
                                    </div>
                                    <div className="flex items-center gap-1.5 font-medium">
                                        <Clock className="w-4 h-4" />
                                        <span className={daysLeft <= 1 ? 'text-accent-red font-bold' : ''}>
                                            {isExpired ? 'Expired' : (daysLeft > 0 ? `${daysLeft} days left` : `${hoursLeft} hours left`)}
                                        </span>
                                    </div>
                                </div>
                            </div>

                            {/* Urgency Progress Bar */}
                            <div className="p-6 bg-surface-2 rounded-2xl border border-border/50">
                                <div className="flex justify-between items-end mb-4">
                                    <div className="space-y-1">
                                        <p className="text-[10px] font-bold uppercase tracking-widest text-text-muted">Ecosystem Urgency</p>
                                        <p className="text-lg font-medium">{signal.signalStrength} Priority</p>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-2xl font-mono font-bold">{Math.round(progressPercent)}%</p>
                                        <p className="text-[10px] uppercase font-bold text-text-muted">Life remaining</p>
                                    </div>
                                </div>
                                <div className="w-full h-3 bg-white/50 rounded-full overflow-hidden border border-border/30">
                                    <motion.div 
                                        initial={{ width: 0 }}
                                        animate={{ width: `${progressPercent}%` }}
                                        transition={{ duration: 1.5, ease: "easeOut" }}
                                        className={`h-full rounded-full ${
                                            daysLeft <= 1 ? "bg-accent-red" : "bg-primary"
                                        }`}
                                    />
                                </div>
                            </div>

                            {/* Description Section */}
                            <div className="space-y-4">
                                <h3 className="text-[10px] font-bold uppercase tracking-widest text-primary/60">Signal Details</h3>
                                <div className="prose prose-slate max-w-none">
                                    <p className="text-lg text-text-secondary leading-relaxed whitespace-pre-wrap">
                                        {signal.description}
                                    </p>
                                </div>
                            </div>

                            {/* Stats & Metadata */}
                            <div className="grid grid-cols-3 gap-4 pt-6 border-t border-border">
                                <div className="text-center p-4 rounded-xl hover:bg-surface-2 transition-colors">
                                    <p className="text-2xl font-mono font-bold">{signal.viewCount || 0}</p>
                                    <p className="text-[10px] uppercase font-bold text-text-muted tracking-widest">Views</p>
                                </div>
                                <div className="text-center p-4 rounded-xl hover:bg-surface-2 transition-colors">
                                    <p className="text-2xl font-mono font-bold">{signal.responseCount || 0}</p>
                                    <p className="text-[10px] uppercase font-bold text-text-muted tracking-widest">Responses</p>
                                </div>
                                <div className="text-center p-4 rounded-xl hover:bg-surface-2 transition-colors">
                                    <p className="text-2xl font-mono font-bold">{signal.offerCount || 0}</p>
                                    <p className="text-[10px] uppercase font-bold text-text-muted tracking-widest">Offers</p>
                                </div>
                            </div>

                            {/* Engagement Section */}
                            <div className="pt-8 space-y-6">
                                <div className="flex items-center justify-between">
                                    <h2 className="text-xl font-display">Discussion</h2>
                                    <span className="text-xs font-bold text-text-muted uppercase tracking-widest">
                                        {signal.responseCount || 0} Respondents
                                    </span>
                                </div>

                                {/* Main Reply Box */}
                                <div className="flex gap-4 bg-surface-2 p-4 rounded-2xl border border-border/30 focus-within:border-primary/30 transition-all">
                                    <div className="w-10 h-10 rounded-full bg-white border border-border shrink-0 overflow-hidden relative">
                                        <Image src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(currentUser?.username || 'anon')}`} fill alt="me" unoptimized />
                                    </div>
                                    <div className="flex-1 space-y-3">
                                        <textarea 
                                            placeholder="Can you help with this? Add your response..."
                                            className="w-full bg-transparent border-none outline-none text-sm resize-none min-h-[60px]"
                                            value={commentText}
                                            onChange={(e) => setCommentText(e.target.value)}
                                        />
                                        <div className="flex justify-end">
                                            <button 
                                                disabled={!commentText.trim()}
                                                onClick={() => {
                                                    addComment(signal.id, commentText.trim(), currentUser?.username || 'anonymous')
                                                    setCommentText('')
                                                    if (!alreadyResponded && !isOwner) {
                                                        addResponse({ signalId: signal.id, signalTitle: signal.title, signalUsername: signal.username, signalCategory: signal.category })
                                                    }
                                                }}
                                                className="bg-black text-white px-6 py-2 rounded-full text-xs font-bold hover:bg-primary transition-all disabled:opacity-40"
                                            >
                                                Post Response
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                {/* Comments Display (Mocked/Static for now, using the local signal storage structure) */}
                                <div className="space-y-6 pl-4">
                                    {((signal as any).comments || []).length === 0 ? (
                                        <div className="text-center py-12 text-text-muted space-y-2">
                                            <MessageSquare className="w-8 h-8 mx-auto opacity-20" />
                                            <p className="text-sm">No discussion yet. Be the first to spark engagement!</p>
                                        </div>
                                    ) : (
                                        (signal as any).comments.map((comment: any) => (
                                            <div key={comment.id} className="flex gap-4 group">
                                                <div className="w-8 h-8 rounded-full bg-surface-2 border border-border shrink-0 overflow-hidden relative">
                                                    <Image src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(comment.username)}`} fill alt={comment.username} unoptimized />
                                                </div>
                                                <div className="flex-1 space-y-1">
                                                    <div className="flex items-center gap-2">
                                                        <span className="font-bold text-sm">@{comment.username}</span>
                                                        <span className="text-[10px] text-text-muted">{new Date(comment.timestamp).toLocaleDateString()}</span>
                                                    </div>
                                                    <p className="text-sm text-text-secondary leading-relaxed">{comment.text}</p>
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Sidebar Information */}
                        <div className="space-y-6">
                            {/* Owner Card */}
                            <div className="bg-white border border-border rounded-2xl overflow-hidden shadow-sm sticky top-8">
                                <div className="h-4 bg-primary relative" />
                                <div className="px-6 pb-6 pt-4 space-y-4">
                                    <div className="flex items-center gap-4">
                                        <VerifiedAvatar
                                            username={signal.username}
                                            avatarUrl={owner?.avatarUrl}
                                            plan={owner?.plan}
                                            size="w-16 h-16"
                                            badgeSize="w-5 h-5"
                                            className="rounded-2xl"
                                        />
                                        <div>
                                            <h3 className="text-xl font-display leading-tight">{owner?.name || signal.username}</h3>
                                            <p className="text-xs text-text-muted font-mono uppercase tracking-widest mt-0.5">@{signal.username}</p>
                                        </div>
                                    </div>

                                    {owner && (
                                        <div className="space-y-3">
                                            <div className="flex items-center gap-2 text-xs text-text-secondary">
                                                <MapPin className="w-3.5 h-3.5" />
                                                <span>{owner.city || 'India'}, {owner.state || 'Ecosystem'}</span>
                                            </div>
                                            <div className="flex items-center gap-2 text-xs text-text-secondary">
                                                <Zap className="w-3.5 h-3.5" />
                                                <span className="capitalize">{owner.role} • {owner.plan} member</span>
                                            </div>
                                        </div>
                                    )}

                                    <div className="pt-2 flex flex-col gap-2">
                                        <Link 
                                            href={`/profile/${signal.username}`}
                                            className="w-full border border-border py-2.5 rounded-lg text-sm font-medium hover:bg-surface-2 transition-all flex items-center justify-center gap-2"
                                        >
                                            View Full Profile
                                        </Link>
                                        {!isOwner && (
                                            <button 
                                                onClick={async () => {
                                                    if (!alreadyConnected && !alreadyPending) {
                                                        if (!token) { alert('Please login to connect'); return; }
                                                        try {
                                                            await sendRequest(signal.id, 'I want to connect!', token);
                                                        } catch (err) { /* handled */ }
                                                    }
                                                }}
                                                className={`w-full py-2.5 rounded-lg text-sm font-bold transition-all flex items-center justify-center gap-2 ${
                                                    alreadyConnected 
                                                        ? 'bg-green-50 text-green-600 border border-green-200' 
                                                        : alreadyPending
                                                        ? 'bg-surface-2 text-text-muted border border-border cursor-not-allowed'
                                                        : 'bg-black text-white hover:bg-primary'
                                                }`}
                                            >
                                                {alreadyConnected ? (
                                                    <><CheckCheck className="w-4 h-4" /> Connected</>
                                                ) : alreadyPending ? (
                                                    'Pending Request'
                                                ) : (
                                                    <><UserPlus className="w-4 h-4" /> Connect with {owner?.name?.split(' ')[0] || 'User'}</>
                                                )}
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* Help Actions Sticky Bar (Mobile only) */}
                            <div className="fixed bottom-0 left-0 right-0 p-4 bg-white/80 backdrop-blur-md border-t border-border flex gap-3 lg:hidden z-40">
                                <button className="flex-1 bg-primary text-white py-4 rounded-xl font-bold text-sm shadow-lg shadow-primary/20">
                                    I Can Help
                                </button>
                                <button className="flex-1 bg-black text-white py-4 rounded-xl font-bold text-sm">
                                    Respond
                                </button>
                            </div>
                        </div>
                    </div>
                </main>
                <MobileBottomNav />
            </div>
        </div>
    )
}
