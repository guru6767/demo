"use client"

import { MessageSquare, Zap, UserPlus, MoreHorizontal, Edit3, Trash2, CheckCheck, BarChart2 } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import Image from 'next/image'
import Link from 'next/link'
import { useState, useRef, useEffect } from 'react'
import { useSignalStore, Signal, Comment, getSignalExpiration } from '@/store/useSignalStore'
import { useAuthStore } from '@/store/useAuthStore'
import { useNetworkStore } from '@/store/useNetworkStore'
import { useResponseStore } from '@/store/useResponseStore'
import RaiseSignalModal from './RaiseSignalModal'
import InsightsModal from './InsightsModal'
import HelpModal from './HelpModal'
import VerifiedAvatar from './VerifiedAvatar'
import { signalsApi } from '@/lib/apiClient'

// ── @Mention hook — searches all known usernames in the store ────────────────
function useMentionSuggestions(text: string) {
    const { signals } = useSignalStore()
    const atIdx = text.lastIndexOf('@')
    if (atIdx === -1) return { suggestions: [], query: '', atIdx: -1 }
    const query = text.slice(atIdx + 1).toLowerCase()
    const allUsers = Array.from(new Set(signals.map(s => s.username)))
    const suggestions = query.length >= 1
        ? allUsers.filter(u => u.toLowerCase().includes(query)).slice(0, 5)
        : []
    return { suggestions, query, atIdx }
}

// ── Reply Input with @mention ─────────────────────────────────────────────────
function ReplyInput({ placeholder, onSubmit, onCancel }: {
    placeholder: string
    onSubmit: (text: string) => void
    onCancel: () => void
}) {
    const [text, setText] = useState(placeholder.startsWith('Reply') ? '' : '')
    const [preText] = useState(placeholder.startsWith('@') ? `@${placeholder.split('@')[1]?.split('...')[0]} ` : '')
    const [value, setValue] = useState(preText)
    const { suggestions, atIdx } = useMentionSuggestions(value)
    const inputRef = useRef<HTMLInputElement>(null)
    useEffect(() => { inputRef.current?.focus() }, [])

    const applyMention = (username: string) => {
        const before = value.slice(0, atIdx)
        setValue(`${before}@${username} `)
        inputRef.current?.focus()
    }

    const submit = () => {
        if (!value.trim()) return
        onSubmit(value.trim())
        setValue('')
    }

    return (
        <div className="mt-2 relative">
            {suggestions.length > 0 && (
                <div className="absolute bottom-full mb-1 left-0 bg-white border border-border shadow-lg rounded-xl z-30 w-48 py-1 overflow-hidden">
                    {suggestions.map(u => (
                        <button key={u} onMouseDown={(e) => { e.preventDefault(); applyMention(u) }}
                            className="w-full text-left px-3 py-1.5 text-xs hover:bg-surface-2 flex items-center gap-2">
                            <span className="font-bold text-black">@{u}</span>
                        </button>
                    ))}
                </div>
            )}
            <div className="flex gap-2 items-center">
                <input
                    ref={inputRef}
                    type="text"
                    placeholder={placeholder}
                    className="flex-1 bg-surface-2 rounded-full px-3 py-1 text-xs outline-none border border-border focus:border-primary"
                    value={value}
                    onChange={e => setValue(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') submit() }}
                />
                <button onClick={submit} disabled={!value.trim()} className="text-primary font-bold text-xs disabled:opacity-40">Post</button>
                <button onClick={onCancel} className="text-text-muted text-xs">Cancel</button>
            </div>
        </div>
    )
}

// ── Recursive Comment/Reply Row ───────────────────────────────────────────────
function CommentRow({ comment, signalId, currentUser, depth = 0 }: {
    comment: Comment; signalId: string; currentUser: string | null | undefined; depth?: number
}) {
    const [showReply, setShowReply] = useState(false)
    const avatarSize = depth === 0 ? 'w-6 h-6' : 'w-5 h-5'
    const textSize = depth === 0 ? 'text-sm' : 'text-xs'

    const handleReply = (text: string) => {
        if (!currentUser) return
        
        // If replying to a reply, prefix their handle so it's clear who is being addressed
        const finalString = depth > 0 && !text.startsWith(`@${comment.username}`) 
            ? `@${comment.username} ${text}` 
            : text;
            
        useSignalStore.getState().addReply(signalId, comment.id, finalString, currentUser)
        setShowReply(false)
    }

    return (
        <div className={`flex gap-2 ${textSize}`}>
            <div className={`${avatarSize} rounded-full bg-surface-2 overflow-hidden shrink-0 mt-0.5 relative`}>
                <Image src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(comment.username)}`} alt={comment.username} fill className="object-cover" />
            </div>
            <div className="flex-1 min-w-0">
                <p>
                    <span className="font-bold mr-1.5 text-black cursor-pointer hover:underline">@{comment.username}</span>
                    <span className="text-text-secondary">{comment.text}</span>
                </p>
                <div className="flex items-center gap-3 mt-0.5">
                    <p className="text-[10px] text-text-muted">
                        {new Date(comment.timestamp).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}
                    </p>
                    {currentUser && (
                        <button onClick={() => setShowReply(v => !v)} className="text-[10px] font-bold text-text-muted hover:text-primary transition-colors">
                            Reply
                        </button>
                    )}
                </div>

                {/* Nested replies — always rendered recursively */}
                {(comment.replies || []).length > 0 && (
                    <div className="mt-2 space-y-2 pl-3 border-l-2 border-border">
                        {(comment.replies || []).map(reply => (
                            <CommentRow key={reply.id} comment={reply} signalId={signalId} currentUser={currentUser} depth={depth + 1} />
                        ))}
                    </div>
                )}

                {showReply && (
                    <ReplyInput
                        placeholder={`Reply to @${comment.username}...`}
                        onSubmit={handleReply}
                        onCancel={() => setShowReply(false)}
                    />
                )}
            </div>
        </div>
    )
}
// ─────────────────────────────────────────────────────────────────────────────

// Keep old name as alias for the main comment list
function CommentThread({ comment, signalId, currentUser }: { comment: Comment; signalId: string; currentUser: string | null | undefined }) {
    return <CommentRow comment={comment} signalId={signalId} currentUser={currentUser} depth={0} />
}

// Known role keywords — only these are treated as the role suffix
const ROLE_KEYWORDS = new Set(['founder', 'investor', 'mentor', 'talent', 'expert'])

// Formats username for display:
//   'sagar_g_founder'  → { handle: '@sagar_g',  role: 'Founder' }
//   'krishna_k88_founder' → { handle: '@krishna_k88', role: 'Founder' }
//   'talent' / '_talent' / any unrecognised → '@username' (plain, no badge)
// Simplified display: we now just show the full username with an @
function formatUsername(username: string): string {
    return username ? `@${username}` : '@'
}

interface SignalCardProps {
    id: string
    title: string
    username: string
    timeAgo: string
    category: string
    description: string
    strength: string
    stats: {
        responses: number
        offers: number
        views: number
    }
    hideViews?: boolean
    userPlan?: string
    createdAt?: number | string
    onRefresh?: () => void
}

export default function SignalCard({ id, title, username, timeAgo, category, description, strength, stats, hideViews = false, userPlan = 'free', createdAt, onRefresh }: SignalCardProps) {
    const { user, token } = useAuthStore()
    const currentUser = user?.username
    const { deleteSignal, signals } = useSignalStore()
    const { connections, sentRequests, pendingRequests, sendRequest } = useNetworkStore()
    const { addResponse, hasResponded } = useResponseStore()
    const [showDropdown, setShowDropdown] = useState(false)
    const [isEditModalOpen, setIsEditModalOpen] = useState(false)
    const [isInsightsModalOpen, setIsInsightsModalOpen] = useState(false)
    const [isHelpModalOpen, setIsHelpModalOpen] = useState(false)
    const [showComments, setShowComments] = useState(false)
    const [commentText, setCommentText] = useState('')
    const [addedToNetwork, setAddedToNetwork] = useState(false)
    const [justResponded, setJustResponded] = useState(false)
    const [isDeleting, setIsDeleting] = useState(false)
    const dropdownRef = useRef<HTMLDivElement>(null)
    const safeConnections = Array.isArray(connections) ? connections : []
    const safeSent = Array.isArray(sentRequests) ? sentRequests : []
    const safePending = Array.isArray(pendingRequests) ? pendingRequests : []

    const alreadyConnected = safeConnections.some(c => c.username === username)
    const alreadyPending = safeSent.some(r => r.username === username && r.status === 'pending')
    const alreadyResponded = hasResponded(id)

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setShowDropdown(false)
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, [])
    const isOwner = currentUser === username
    const storeSignal = signals.find(s => s.id === id)
    
    // Fallback object so modals can open even for backend-only signals not in local store
    const currentSignal: Signal = storeSignal || {
        id,
        title,
        username,
        timeAgo,
        category,
        description,
        strength,
        status: 'Active',
        stats: stats || { responses: 0, offers: 0, views: 0 },
        userPlan,
        createdAt: typeof createdAt === 'string' ? new Date(createdAt).getTime() : createdAt
    } as Signal

    // Days left calculation using helper
    const { isExpired, daysLeft, hoursLeft, totalDuration, progressPercent } = getSignalExpiration(currentSignal || { strength, createdAt } as any);
    
    const strengthColor: Record<string, string> = {
        Normal: 'bg-accent-blue',
        High: 'bg-accent-yellow',
        Critical: 'bg-accent-red',
        Low: 'bg-text-muted'
    }
    const colorClass = strengthColor[strength] || 'bg-text-muted'
    
    // Social Proof: Show if a connection has responded, or the user themselves
    const connectionRespondent = currentSignal?.comments?.find(c => 
        safeConnections.some(conn => conn.username === c.username)
    )?.username;

    const respondentToShow = connectionRespondent || (currentSignal?.comments?.some(c => c.username === currentUser) ? currentUser : null);

    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-white border border-border p-5 rounded-xl mb-4 hover:border-text-muted transition-all group shadow-sm hover:shadow-md"
        >
            <div className="flex justify-between items-start mb-4">
                <Link href={`/profile/${username}`} className="flex items-center gap-3 group/profile hover:opacity-80 transition-opacity">
                    <VerifiedAvatar
                        username={username}
                        plan={userPlan}
                        size="w-10 h-10"
                        badgeSize="w-4 h-4"
                    />
                    <div>
                    <div className="flex items-center gap-2 overflow-hidden">
                        <span className="text-sm font-bold truncate hover:underline cursor-pointer">{username}</span>
                        <span className="text-text-muted text-xs shrink-0">•</span>
                        <span className="text-text-muted text-[10px] font-bold uppercase tracking-widest shrink-0">{timeAgo}</span>
                    </div>
                    </div>
                </Link>
                <div className="flex items-center gap-2">
                    <span className="text-[10px] uppercase tracking-wider font-bold bg-primary text-white px-2 py-0.5 rounded-full">
                        {category}
                    </span>
                    {isOwner ? (
                        <div className="relative" ref={dropdownRef}>
                            <button 
                                onClick={(e) => { e.stopPropagation(); setShowDropdown(!showDropdown); }} 
                                className={`text-text-muted hover:text-primary p-1 rounded-md transition-all ${showDropdown ? 'bg-surface-2 text-primary' : 'hover:bg-surface-2'}`}
                            >
                                <MoreHorizontal className="w-5 h-5" />
                            </button>
                            <AnimatePresence>
                                {showDropdown && (
                                    <motion.div 
                                        initial={{ opacity: 0, scale: 0.95, y: -10 }}
                                        animate={{ opacity: 1, scale: 1, y: 0 }}
                                        exit={{ opacity: 0, scale: 0.95, y: -10 }}
                                        transition={{ duration: 0.15 }}
                                        className="absolute right-0 top-full mt-1 w-40 bg-white border border-border rounded-lg shadow-lg z-20 py-1 flex flex-col overflow-hidden"
                                    >
                                        <button 
                                            onClick={(e) => { e.stopPropagation(); setIsInsightsModalOpen(true); setShowDropdown(false); }}
                                            className="px-4 py-2.5 text-sm text-left hover:bg-surface-2 transition-colors w-full flex items-center gap-2 text-text-secondary hover:text-black font-medium"
                                        >
                                            <BarChart2 className="w-4 h-4" /> Insights
                                        </button>
                                        <button 
                                            onClick={(e) => { e.stopPropagation(); setIsEditModalOpen(true); setShowDropdown(false); }}
                                            className="px-4 py-2.5 text-sm text-left hover:bg-surface-2 transition-colors w-full flex items-center gap-2 text-text-secondary hover:text-black font-medium"
                                        >
                                            <Edit3 className="w-4 h-4" /> Modify Signal
                                        </button>
                                        <button 
                                            disabled={isDeleting}
                                            onClick={async (e) => {
                                                e.stopPropagation();
                                                if (!confirm('Permanently delete this signal?')) return;
                                                
                                                setIsDeleting(true);
                                                const authToken = token || (user?.username ? `dev_${user.username}` : '');
                                                const { error } = await signalsApi.delete(id, authToken);
                                                
                                                if (!error) {
                                                    deleteSignal(id); // remove from local store too
                                                    if (onRefresh) onRefresh();
                                                } else {
                                                    alert('Failed to delete: ' + error);
                                                }
                                                setIsDeleting(false);
                                                setShowDropdown(false);
                                            }}
                                            className="px-4 py-2.5 text-sm text-left hover:bg-red-50 transition-colors w-full flex items-center gap-2 text-accent-red font-medium disabled:opacity-50"
                                        >
                                            <Trash2 className="w-4 h-4" /> {isDeleting ? 'Deleting...' : 'Delete Signal'}
                                        </button>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </div>
                    ) : (
                        <button className="text-text-muted hover:text-primary p-1 rounded-md hover:bg-surface-2 transition-all">
                            <MoreHorizontal className="w-5 h-5" />
                        </button>
                    )}
                </div>
            </div>

            <Link href={`/signals/${id}`} className="block group/link cursor-pointer">
                <h3 className="text-lg font-medium mb-2 group-hover/link:text-primary group-hover/link:underline transition-colors">{title}</h3>
                <p className="text-text-secondary text-sm mb-4 line-clamp-2">{description}</p>
            </Link>

            <div className="flex items-center gap-6 mb-5">
                <div className="flex flex-col">
                    <span className="text-[10px] text-text-muted uppercase">Responses</span>
                    <span className="font-mono text-sm">{stats.responses}</span>
                </div>
                <div className="flex flex-col">
                    <span className="text-[10px] text-text-muted uppercase">Offers</span>
                    <span className="font-mono text-sm">{stats.offers}</span>
                </div>
                {!hideViews && (
                    <div className="flex flex-col">
                        <span className="text-[10px] text-text-muted uppercase">Views</span>
                        <span className="font-mono text-sm">{stats.views}</span>
                    </div>
                )}
            </div>
            
            {/* Urgency Progress Bar */}
            <div className="mb-5 relative">
                <div className="w-full h-1.5 bg-surface-2 overflow-hidden mb-1 flex">
                    <motion.div 
                        initial={{ width: 0 }}
                        animate={{ width: `${progressPercent}%` }}
                        transition={{ duration: 1, delay: 0.2, ease: "easeOut" }}
                        className={`h-full rounded-r-md transition-colors ${
                            daysLeft <= 1 ? "bg-red-500" :
                            daysLeft >= totalDuration - 2 ? "bg-green-500" :
                            "bg-gradient-to-r from-yellow-400 to-orange-500"
                        }`}
                    />
                </div>
                <div className={`text-[10px] uppercase tracking-widest font-bold float-right ${
                    daysLeft <= 1 ? "text-red-500" :
                    daysLeft >= totalDuration - 2 ? "text-green-600" :
                    "text-orange-500"
                }`}>
                    {daysLeft > 0 ? `${daysLeft} Days Left` : `${hoursLeft} Hours Left`}
                </div>
                <div className="clear-both" />
            </div>

            {/* Social Proof Line */}
            {respondentToShow && (
                <div className="mb-4 flex items-center gap-2">
                    <div className="flex -space-x-2">
                        <div className="w-5 h-5 rounded-full border border-white bg-surface-2 relative overflow-hidden">
                            <Image src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(respondentToShow)}`} alt="proof" fill className="object-cover" unoptimized />
                        </div>
                    </div>
                    <p className="text-[11px] text-text-secondary">
                        Responded by <span className="font-bold text-black border-b border-black/20">{respondentToShow === currentUser ? 'you' : `@${respondentToShow}`}</span>
                        {stats.responses > 1 && ` and ${stats.responses - 1} others`}
                    </p>
                </div>
            )}

            {/* Actions */}

            <div className="flex gap-2">
                <button onClick={() => setIsHelpModalOpen(true)} className="flex-1 bg-primary text-white py-2.5 rounded-md text-sm font-medium flex items-center justify-center gap-2 hover:opacity-90 transition-all">
                    <Zap className="w-4 h-4 fill-white" /> Help
                </button>
                <button
                    onClick={() => setShowComments(!showComments)}
                    className={`flex-1 border py-2.5 rounded-md text-sm font-medium flex items-center justify-center gap-2 transition-all ${
                        showComments
                            ? 'border-black bg-black text-white'
                            : 'border-border hover:bg-surface-2'
                    }`}
                >
                    <MessageSquare className="w-4 h-4" />
                    Respond
                </button>
                <button
                    onClick={async () => {
                        if (!isOwner && !alreadyConnected && !alreadyPending && !addedToNetwork) {
                            try {
                                await sendRequest(id, 'I want to connect!', token || 'mock-token', currentSignal?.username);
                                setAddedToNetwork(true);
                            } catch (err) {
                                // Error handled in store
                            }
                        }
                    }}
                    disabled={isOwner}
                    className={`px-3 border rounded-md transition-all duration-300 ${
                        isOwner
                            ? 'border-border opacity-30 cursor-not-allowed'
                            : alreadyConnected
                                ? 'bg-black text-white'
                                : alreadyPending || addedToNetwork
                                    ? 'bg-black text-white shadow-md'
                                    : 'border-border hover:bg-surface-2'
                    }`}
                    title={isOwner ? 'Your own signal' : alreadyConnected ? 'Connected' : alreadyPending ? 'Request sent — pending' : 'Send connection request'}
                >
                    {alreadyConnected ? (
                        <CheckCheck className="w-4 h-4" />
                    ) : alreadyPending || addedToNetwork ? (
                        <motion.div 
                            initial={{ scale: 0.5, opacity: 0 }} 
                            animate={{ scale: [0.8, 1.2, 1], rotate: [0, -10, 10, -10, 0], opacity: 1 }} 
                            transition={{ type: "tween", duration: 0.4 }}
                        >
                            <CheckCheck className="w-4 h-4" />
                        </motion.div>
                    ) : (
                        <UserPlus className="w-4 h-4" />
                    )}
                </button>
            </div>
            
            {/* Instagram Style Threaded Comments Section */}
            <AnimatePresence>
                {showComments && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="mt-4 pt-4 border-t border-border overflow-hidden"
                    >
                        <div className="space-y-4 max-h-80 overflow-y-auto pr-2" style={{ scrollbarWidth: 'thin' }}>
                            {(currentSignal?.comments || []).length === 0 ? (
                                <p className="text-center text-xs text-text-muted py-4">No responses yet. Be the first to respond!</p>
                            ) : (
                                (currentSignal?.comments || []).map(comment => (
                                    <CommentThread
                                        key={comment.id}
                                        comment={comment}
                                        signalId={id}
                                        currentUser={currentUser}
                                    />
                                ))
                            )}
                        </div>

                        {/* Main Comment Input */}
                        <div className="mt-4 flex gap-2 items-center border-t border-border pt-3">
                            <div className="w-8 h-8 rounded-full border border-border bg-surface-2 relative overflow-hidden shrink-0">
                                <Image src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(currentUser || 'user')}`} alt="me" fill className="object-cover" unoptimized />
                            </div>
                            <input
                                type="text"
                                placeholder="Add a comment..."
                                className="flex-1 bg-transparent border-none outline-none text-sm placeholder:text-text-muted ml-2"
                                value={commentText}
                                onChange={(e) => setCommentText(e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter' && commentText.trim()) {
                                        useSignalStore.getState().addComment(id, commentText.trim(), currentUser || 'user')
                                        setCommentText('')
                                        if (!alreadyResponded && !isOwner) {
                                            addResponse({ signalId: id, signalTitle: title, signalUsername: username, signalCategory: category })
                                        }
                                    }
                                }}
                            />
                            <button 
                                disabled={!commentText.trim()}
                                onClick={() => {
                                    if (commentText.trim()) {
                                        useSignalStore.getState().addComment(id, commentText.trim(), currentUser || 'user')
                                        setCommentText('')
                                        if (!alreadyResponded && !isOwner) {
                                            addResponse({ signalId: id, signalTitle: title, signalUsername: username, signalCategory: category })
                                        }
                                    }
                                }}
                                className="text-primary font-bold text-sm disabled:opacity-50 transition-opacity"
                            >
                                Post
                            </button>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
            
            {currentSignal && (
                <RaiseSignalModal 
                    isOpen={isEditModalOpen} 
                    onClose={() => setIsEditModalOpen(false)} 
                    editSignal={currentSignal} 
                />
            )}
            {currentSignal && (
                <InsightsModal 
                    isOpen={isInsightsModalOpen} 
                    onClose={() => setIsInsightsModalOpen(false)} 
                    stats={stats}
                    signalTitle={title}
                />
            )}
            <HelpModal 
                isOpen={isHelpModalOpen} 
                onClose={() => setIsHelpModalOpen(false)} 
                signalId={id} 
                signalTitle={title} 
            />
        </motion.div>
    )
}
