"use client"

import Sidebar from '@/components/feed/Sidebar'
import MobileBottomNav from '@/components/feed/MobileBottomNav'
import { MapPin, Globe, Twitter, Linkedin, Github, Signal, Zap, Camera, Upload, Users, BadgeCheck, Star, Edit3, Check, X, Link as LinkIcon, Clock, CreditCard, Receipt, AlertCircle } from 'lucide-react'
import Image from 'next/image'
import { useState, useRef, useEffect } from 'react'
import { useAuthStore } from '@/store/useAuthStore'
import { useSignalStore, getSignalExpiration } from '@/store/useSignalStore'
import { useNetworkStore } from '@/store/useNetworkStore'
import { useRatingStore } from '@/store/useRatingStore'
import { useResponseStore } from '@/store/useResponseStore'
import { usePaymentStore } from '@/store/usePaymentStore'
import Link from 'next/link'
import { motion, AnimatePresence } from 'framer-motion'
import { useRouter } from 'next/navigation'
import { useLocalUserStore } from '@/store/useLocalUserStore'

export default function UserProfile() {
    const router = useRouter()
    const { user, isAuthenticated, isLoading, updateUser } = useAuthStore()
    const { updateUserRecord } = useLocalUserStore()

    const formatURL = (url: string | null | undefined) => {
        if (!url) return '#'
        return url.startsWith('http') ? url : `https://${url}`
    }

    const extractHandle = (url: string | null | undefined, prefix = '@') => {
        if (!url) return ''
        const parts = url.split('/').filter(Boolean)
        const lastPart = parts[parts.length - 1] || ''
        return lastPart.startsWith('@') ? lastPart : `${prefix}${lastPart}`
    }

    const {
        isVerified = false,
        subscription = 'Free',
        name = '',
        username = '',
        role = '',
        city = '',
        bio = '',
        websiteUrl = '',
        linkedinUrl = '',
        twitterUrl = '',
        githubUrl = '',
        avatarUrl = null
    } = user || {}

    const { signals } = useSignalStore()
    const { connections } = useNetworkStore()
    const { getAverageRating, getRatingsFor } = useRatingStore()
    const { responses } = useResponseStore()

    const SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000
    const mySignals = signals.filter(s => s.username === username)
    const activeSignals = mySignals.filter(s => s.status === 'Active' && !getSignalExpiration(s).isExpired)
    const pastSignals = mySignals.filter(s => s.status === 'Solved' || getSignalExpiration(s).isExpired)
    const avgRating = getAverageRating(username)
    const myRatings = getRatingsFor(username)
    const [activeTab, setActiveTab] = useState<'active' | 'past' | 'responses' | 'payments'>('active')
    const [showAllActiveSignals, setShowAllActiveSignals] = useState(false)
    const { records: paymentHistory } = usePaymentStore()

    const [isEditing, setIsEditing] = useState(false)
    const [isEditingSocial, setIsEditingSocial] = useState(false)
    
    const [socialForm, setSocialForm] = useState({ linkedinUrl, twitterUrl, githubUrl })
    const [editForm, setEditForm] = useState({
        name,
        role,
        city,
        bio,
        websiteUrl,
        linkedinUrl,
        twitterUrl,
        githubUrl,
        avatarUrl,
        handleBase: username
            ? username.split('_').slice(0, -1).join('_')
            : (name ? name.split(' ')[0].toLowerCase() : '')
    })

    useEffect(() => {
        if (!isLoading && !isAuthenticated) {
            router.push('/auth')
        }
    }, [isAuthenticated, isLoading, router])

    useEffect(() => {
        if (user) {
            setSocialForm({ linkedinUrl: user.linkedinUrl || '', twitterUrl: user.twitterUrl || '', githubUrl: user.githubUrl || '' })
            setEditForm({
                name: user.name || '',
                role: user.role || '',
                city: user.city || '',
                bio: user.bio || '',
                websiteUrl: user.websiteUrl || '',
                linkedinUrl: user.linkedinUrl || '',
                twitterUrl: user.twitterUrl || '',
                githubUrl: user.githubUrl || '',
                avatarUrl: user.avatarUrl || null,
                handleBase: user.username ? user.username.split('_').slice(0, -1).join('_') : user.name.split(' ')[0]?.toLowerCase() || ''
            })
        }
    }, [user])

    const fileInputRef = useRef<HTMLInputElement>(null)
    const onFileChange = (e: React.ChangeEvent<HTMLInputElement>, type: 'profile') => {
        const file = e.target.files?.[0]
        if (file) {
            const url = URL.createObjectURL(file)
            setEditForm(prev => ({ ...prev, avatarUrl: url }))
        }
    }

    const handleSave = () => {
        if (editForm.websiteUrl && !isValidUrl(editForm.websiteUrl)) {
            alert('Invalid Website URL')
            return
        }
        const formattedRole = editForm.role.toLowerCase().trim().replace(/[\s/]+/g, '').replace(/[^a-z0-9]+/g, '')
        const formattedBase = editForm.handleBase.toLowerCase().trim().replace(/\s+/g, '_').replace(/[^a-z0-9_]+/g, '')
        const newUsername = `${formattedBase}_${formattedRole}`

        updateUser({ ...editForm, username: newUsername })
        // Keep local user store in sync so signals & login still match
        if (user?.email) updateUserRecord(user.email, { username: newUsername })
        setIsEditing(false)
    }

    const handleCancel = () => {
        if (user) {
            setEditForm({ 
                name: user.name || '', 
                role: user.role || '', 
                city: user.city || '', 
                bio: user.bio || '', 
                websiteUrl: user.websiteUrl || '', 
                linkedinUrl: user.linkedinUrl || '', 
                twitterUrl: user.twitterUrl || '', 
                githubUrl: user.githubUrl || '', 
                avatarUrl: user.avatarUrl || null, 
                coverUrl: user.coverUrl || null, 
                handleBase: user.username ? user.username.split('_').slice(0, -1).join('_') : user.name.split(' ')[0]?.toLowerCase() || '' 
            })
        }
        setIsEditing(false)
    }

    const [socialError, setSocialError] = useState('')

    const isValidUrl = (url: string) => {
        if (!url) return true // Allow empty
        try {
            new URL(url.startsWith('http') ? url : `https://${url}`)
            return true
        } catch (_) {
            return false
        }
    }

    const handleSaveSocial = () => {
        if (!isValidUrl(socialForm.linkedinUrl)) {
            setSocialError('Invalid LinkedIn URL')
            return
        }
        if (!isValidUrl(socialForm.githubUrl)) {
            setSocialError('Invalid GitHub URL')
            return
        }
        // Twitter can be a handle or URL, but user said "only accept links"
        if (socialForm.twitterUrl && !isValidUrl(socialForm.twitterUrl)) {
            setSocialError('Invalid Twitter URL')
            return
        }

        setSocialError('')
        updateUser({ linkedinUrl: socialForm.linkedinUrl, twitterUrl: socialForm.twitterUrl, githubUrl: socialForm.githubUrl })
        setIsEditingSocial(false)
    }

    const handleCancelSocial = () => {
        setSocialError('')
        if (user) {
            setSocialForm({ linkedin: user.linkedin || '', twitter: user.twitter || '', github: user.github || '' })
        }
        setIsEditingSocial(false)
    }

    if (!isAuthenticated || !user) return <div className="min-h-screen bg-background flex justify-center items-center text-text-muted">Loading...</div>;

    return (
        <div className="min-h-screen bg-background flex justify-center">
            <div className="max-w-[1400px] w-full flex flex-col md:flex-row pb-16 md:pb-0">
                <Sidebar />

                <input type="file" ref={fileInputRef} className="hidden" onChange={(e) => onFileChange(e, 'profile')} accept="image/*" />

                <main className="flex-1 w-full max-w-[680px] md:border-r border-border min-h-screen p-0">

                    {/* ── Clean Profile Header (no cover) ── */}
                    <div className="px-8 pt-8 pb-6 border-b border-border">
                        <div className="flex items-start gap-6">
                            {/* Avatar */}
                            <div className="relative shrink-0">
                                <div className="w-24 h-24 bg-white rounded-2xl border-2 border-border shadow-lg relative overflow-hidden">
                                    {(isEditing ? editForm.avatarUrl : avatarUrl) ? (
                                        <Image src={isEditing ? editForm.avatarUrl! : avatarUrl!} alt="Profile" fill className="object-cover" />
                                    ) : (
                                        <div className="w-full h-full bg-surface-2 flex items-center justify-center">
                                            <Users className="w-10 h-10 text-text-muted" />
                                        </div>
                                    )}
                                    {isEditing && (
                                        <button onClick={() => handleUpload('profile')} className="absolute inset-0 bg-black/50 flex flex-col items-center justify-center text-white">
                                            <Camera className="w-5 h-5 mb-1" /><span className="text-[9px] font-bold uppercase">Change</span>
                                        </button>
                                    )}
                                </div>
                            </div>

                            {/* Name + details */}
                            <div className="flex-1 min-w-0">
                                {isEditing ? (
                                    <div className="space-y-4">
                                        <div className="flex flex-col sm:flex-row gap-3">
                                            <div className="flex-1">
                                                <label className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1 block">Full Name</label>
                                                <input value={editForm.name} onChange={(e) => setEditForm({ ...editForm, name: e.target.value })} className="w-full bg-surface-1 border border-border p-2 rounded-md font-display text-xl focus:ring-1 focus:ring-primary outline-none" />
                                            </div>
                                            <div className="flex-1">
                                                <label className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1 block">Role</label>
                                                <input value={editForm.role} onChange={(e) => setEditForm({ ...editForm, role: e.target.value })} className="w-full bg-surface-1 border border-border p-2 rounded-md text-sm focus:ring-1 focus:ring-primary outline-none" />
                                            </div>
                                        </div>
                                        <div className="flex flex-col sm:flex-row gap-3">
                                            <div className="flex-1">
                                                <label className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1 block">Location</label>
                                                <div className="relative"><MapPin className="absolute left-2 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" /><input value={editForm.city} onChange={(e) => setEditForm({ ...editForm, city: e.target.value })} className="w-full bg-surface-1 border border-border p-2 pl-8 rounded-md text-sm focus:ring-1 focus:ring-primary outline-none" /></div>
                                            </div>
                                            <div className="flex-1">
                                                <label className="text-[10px] font-bold uppercase tracking-widest text-primary mb-1 block">User Handle (editable)</label>
                                                <input value={editForm.handleBase} onChange={(e) => setEditForm({ ...editForm, handleBase: e.target.value })} className="w-full bg-surface-1 border border-primary/30 p-2 rounded-md font-mono text-sm focus:ring-1 focus:ring-primary outline-none" placeholder="e.g. krishna_k88" />
                                                <p className="text-[10px] text-text-muted mt-1 font-mono">
                                                    Preview: <span className="text-primary font-bold">
                                                        @{editForm.handleBase.toLowerCase().trim().replace(/\s+/g, '_').replace(/[^a-z0-9_]+/g, '') || '…'}_{editForm.role.toLowerCase().trim().replace(/[\s/]+/g, '').replace(/[^a-z0-9]+/g, '') || 'role'}
                                                    </span>
                                                </p>
                                            </div>
                                        </div>
                                        <div>
                                            <label className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1 block">Bio</label>
                                            <textarea value={editForm.bio} onChange={(e) => setEditForm({ ...editForm, bio: e.target.value })} rows={2} className="w-full bg-surface-1 border border-border p-2 rounded-md text-sm focus:ring-1 focus:ring-primary outline-none resize-none" />
                                        </div>
                                        <div className="flex gap-3">
                                            <button onClick={handleSave} className="px-5 py-2 bg-primary text-white rounded-md text-sm font-bold flex items-center gap-2 hover:opacity-90"><Check className="w-4 h-4" /> Save</button>
                                            <button onClick={handleCancel} className="px-5 py-2 border border-border rounded-md text-sm font-bold flex items-center gap-2 hover:bg-surface-2"><X className="w-4 h-4" /> Cancel</button>
                                        </div>
                                    </div>
                                ) : (
                                    <>
                                        <div className="flex items-center gap-2 mb-0.5">
                                            <h1 className="text-2xl font-display">{name}</h1>
                                            {(isVerified || subscription === 'Pro' || subscription === 'Founder') && (
                                                <span title={`${subscription} Verified`} className="relative inline-flex items-center justify-center">
                                                    <BadgeCheck className="w-6 h-6 fill-black text-white" />
                                                </span>
                                            )}
                                        </div>
                                        <p className="text-text-secondary text-sm font-medium mb-1 flex items-center gap-2">
                                            {role} • {city}
                                            <span className="w-1.5 h-1.5 rounded-full bg-accent-green" />
                                            <span className="text-[10px] px-2 py-0.5 bg-surface-2 rounded-full uppercase tracking-tighter font-bold border border-border">{subscription} Account</span>
                                        </p>
                                        {bio ? (
                                            <p className="text-sm text-text-secondary leading-relaxed mb-3 max-w-md">{bio}</p>
                                        ) : (
                                            <p className="text-sm text-text-muted italic mb-3">No bio yet — click Edit Profile to add one.</p>
                                        )}
                                        <div className="flex gap-4 sm:gap-8 mb-3">
                                            <div><p className="text-[10px] uppercase font-bold text-text-muted">Signals</p><p className="text-xl font-mono font-bold">{mySignals.length}</p></div>
                                            <div><p className="text-[10px] uppercase font-bold text-text-muted">Connections</p><p className="text-xl font-mono font-bold">{connections.length}</p></div>
                                            <div>
                                                <p className="text-[10px] uppercase font-bold text-text-muted">Rating</p>
                                                <div className="flex items-center gap-1"><p className="text-xl font-mono font-bold">{avgRating > 0 ? avgRating.toFixed(1) : '—'}</p>{avgRating > 0 && <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />}</div>
                                                <p className="text-[10px] text-text-muted">{myRatings.length} reviews</p>
                                            </div>
                                        </div>
                                        <div className="flex gap-2">
                                            <button onClick={() => setIsEditing(true)} className="px-4 py-2 bg-black text-white rounded-md text-xs font-bold uppercase tracking-widest flex items-center gap-2 hover:opacity-90">
                                                <Edit3 className="w-3.5 h-3.5" /> Edit Profile
                                            </button>
                                            <Link href="/subscription" className="px-4 py-2 border border-border rounded-md text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-2 hover:bg-surface-2">
                                                <Star className="w-3.5 h-3.5" /> {subscription === 'Free' ? 'Upgrade' : 'My Plan'}
                                            </Link>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Signals Feed Section */}
                    <div className="p-8">
                        <div className="flex items-center gap-6 sm:gap-8 border-b border-border mb-8 overflow-x-auto whitespace-nowrap">
                            <button
                                onClick={() => setActiveTab('active')}
                                className={`pb-4 border-b-2 font-bold text-xs uppercase tracking-widest transition-all ${
                                    activeTab === 'active' ? 'border-primary text-primary' : 'border-transparent text-text-muted hover:text-primary'
                                }`}
                            >
                                Active Signals ({activeSignals.length})
                            </button>
                            <button
                                onClick={() => setActiveTab('past')}
                                className={`pb-4 border-b-2 font-bold text-xs uppercase tracking-widest transition-all ${
                                    activeTab === 'past' ? 'border-primary text-primary' : 'border-transparent text-text-muted hover:text-primary'
                                }`}
                            >
                                Past History ({pastSignals.length})
                            </button>
                            <button
                                onClick={() => setActiveTab('responses')}
                                className={`pb-4 border-b-2 font-bold text-xs uppercase tracking-widest transition-all ${
                                    activeTab === 'responses' ? 'border-primary text-primary' : 'border-transparent text-text-muted hover:text-primary'
                                }`}
                            >
                                My Responses ({responses.length})
                            </button>
                            <button
                                onClick={() => setActiveTab('payments')}
                                className={`pb-4 border-b-2 font-bold text-xs uppercase tracking-widest transition-all ${
                                    activeTab === 'payments' ? 'border-primary text-primary' : 'border-transparent text-text-muted hover:text-primary'
                                }`}
                            >
                                Payment History ({paymentHistory.length})
                            </button>
                        </div>

                        <div className="space-y-6">
                            {/* ACTIVE SIGNALS TAB */}
                            {activeTab === 'active' && (
                                activeSignals.length === 0 ? (
                                    <div className="flex flex-col items-center py-16 text-center text-text-muted">
                                        <Zap className="w-10 h-10 mb-3 opacity-30" />
                                        <p className="text-sm">No active signals. Raise one from the Home Feed!</p>
                                    </div>
                                ) : (
                                    <>
                                        {(showAllActiveSignals ? activeSignals : activeSignals.slice(0, 3)).map(signal => (
                                            <div key={signal.id} className="p-6 bg-surface-2 rounded-2xl border border-border group hover:border-primary transition-all mb-4">
                                                <div className="flex justify-between items-start mb-4">
                                                    <span className="text-[10px] px-2 py-0.5 bg-black text-white rounded-full uppercase font-bold tracking-widest">{signal.category}</span>
                                                    <span className="text-[10px] font-bold text-green-500 uppercase tracking-widest">● Active</span>
                                                </div>
                                                <h3 className="text-xl font-display mb-2 group-hover:text-primary transition-colors">{signal.title}</h3>
                                                <p className="text-sm text-text-secondary line-clamp-2">{signal.description}</p>
                                                <div className="mt-4 pt-4 border-t border-border flex justify-between text-[10px] font-bold uppercase tracking-widest">
                                                    <span className="flex items-center gap-1.5"><Zap className="w-3.5 h-3.5 text-primary" /> {signal.stats.responses} Responses</span>
                                                    <span className={`${getSignalExpiration(signal).daysLeft <= 1 ? 'text-red-500' : 'text-text-muted'}`}>
                                                        Expires in {getSignalExpiration(signal).daysLeft > 0 ? `${getSignalExpiration(signal).daysLeft} days` : `${getSignalExpiration(signal).hoursLeft} hours`}
                                                    </span>
                                                </div>
                                            </div>
                                        ))}
                                        {activeSignals.length > 3 && (
                                            <button
                                                onClick={() => setShowAllActiveSignals(!showAllActiveSignals)}
                                                className="w-full py-3 rounded-xl border border-border text-sm font-bold hover:bg-surface-2 transition-all mt-2 text-black"
                                            >
                                                {showAllActiveSignals ? 'View Less' : `View All ${activeSignals.length} Signals`}
                                            </button>
                                        )}
                                    </>
                                )
                            )}

                            {/* PAST HISTORY TAB */}
                            {activeTab === 'past' && (
                                pastSignals.length === 0 ? (
                                    <div className="flex flex-col items-center py-16 text-center text-text-muted">
                                        <Clock className="w-10 h-10 mb-3 opacity-30" />
                                        <p className="text-sm">No past signals yet.</p>
                                    </div>
                                ) : (
                                    pastSignals.map(signal => (
                                        <div key={signal.id} className="p-6 bg-surface-2 rounded-2xl border border-border group transition-all opacity-75">
                                            <div className="flex justify-between items-start mb-4">
                                                <span className="text-[10px] px-2 py-0.5 bg-text-muted text-white rounded-full uppercase font-bold tracking-widest">{signal.category}</span>
                                                {signal.status === 'Solved' ? (
                                                    <span className="text-[10px] font-bold text-green-500 uppercase tracking-widest">✓ Completed</span>
                                                ) : (
                                                    <span className="text-[10px] font-bold text-text-muted uppercase tracking-widest">Expired</span>
                                                )}
                                            </div>
                                            <h3 className="text-xl font-display mb-2">{signal.title}</h3>
                                            <p className="text-sm text-text-secondary line-clamp-2">{signal.description}</p>
                                        </div>
                                    ))
                                )
                            )}

                            {/* MY RESPONSES TAB */}
                            {activeTab === 'responses' && (
                                responses.length === 0 ? (
                                    <div className="flex flex-col items-center py-16 text-center text-text-muted">
                                        <Users className="w-10 h-10 mb-3 opacity-30" />
                                        <p className="text-sm">You haven't responded to any signals yet.</p>
                                    </div>
                                ) : (
                                    responses.map(r => (
                                        <div key={r.id} className="p-6 bg-surface-2 rounded-2xl border border-border group hover:border-primary transition-all">
                                            <div className="flex justify-between items-start mb-4">
                                                <span className="text-[10px] px-2 py-0.5 bg-black text-white rounded-full uppercase font-bold tracking-widest">{r.signalCategory}</span>
                                                <span className="text-[10px] font-bold text-blue-500 uppercase tracking-widest">Responded</span>
                                            </div>
                                            <h3 className="text-xl font-display mb-2 group-hover:text-primary transition-colors">{r.signalTitle}</h3>
                                            <p className="text-xs text-text-muted">Signal by @{r.signalUsername}</p>
                                        </div>
                                    ))
                                )
                            )}

                            {/* PAYMENT HISTORY TAB */}
                            {activeTab === 'payments' && (
                                paymentHistory.length === 0 ? (
                                    <div className="flex flex-col items-center py-20 text-center text-text-muted">
                                        <div className="w-16 h-16 bg-surface-2 rounded-2xl flex items-center justify-center mb-4">
                                            <Receipt className="w-8 h-8 opacity-20" />
                                        </div>
                                        <p className="text-sm font-medium">No payment records found.</p>
                                    </div>
                                ) : (
                                    <div className="space-y-3">
                                        {paymentHistory.map((record) => (
                                            <div key={record.id} className="p-6 bg-white border border-border rounded-3xl group hover:border-black transition-all flex items-center justify-between shadow-sm hover:shadow-md">
                                                <div className="flex items-center gap-5">
                                                    <div className={`w-14 h-14 rounded-2xl flex items-center justify-center transition-colors ${
                                                        record.status === 'Successful' ? 'bg-black text-white' : 'bg-red-50 text-red-500'
                                                    }`}>
                                                        {record.status === 'Successful' ? <Check className="w-6 h-6" /> : <X className="w-6 h-6" />}
                                                    </div>
                                                    <div>
                                                        <div className="flex items-center gap-3 mb-1">
                                                            <h4 className="font-display text-xl tracking-tight text-black">{record.planName}</h4>
                                                            <div className={`px-2 py-0.5 rounded-full text-[8px] font-bold uppercase tracking-[0.2em] border ${
                                                                record.status === 'Successful' ? 'border-black bg-black text-white' : 'border-red-200 bg-red-50 text-red-600'
                                                            }`}>
                                                                {record.status}
                                                            </div>
                                                        </div>
                                                        <div className="flex items-center gap-2 text-text-muted">
                                                            <Clock className="w-3 h-3" />
                                                            <p className="text-[10px] font-mono font-bold uppercase tracking-widest">{record.dateTime}</p>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div className="text-right">
                                                    <div className="flex items-baseline justify-end gap-0.5">
                                                        <span className="text-xs font-bold text-black">{record.currency}</span>
                                                        <span className="text-2xl font-mono font-bold text-black tracking-tighter">{record.amount}</span>
                                                    </div>
                                                    <p className="text-[9px] text-text-muted font-bold uppercase tracking-[0.2em] mt-1">Transaction Settled</p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )
                            )}
                        </div>
                    </div>
                    {/* ── Logout & Delete Account ── */}
                    <div className="px-8 py-6 border-t border-border flex flex-col gap-3">
                        <button
                            onClick={() => { useAuthStore.getState().clearAuth(); router.push('/auth') }}
                            className="w-full flex items-center justify-center gap-2 py-3 border border-border text-text-muted rounded-xl text-sm font-bold uppercase tracking-widest hover:bg-surface-2 transition-colors"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                            Log Out
                        </button>
                        
                        <button
                            onClick={() => {
                                if (window.confirm('Are you absolutely sure you want to delete your account? This cannot be undone.')) {
                                    const store = useLocalUserStore.getState()
                                    // Make sure we have the active user's email
                                    if (user) {
                                        store.deleteUser(user.email)
                                    } else {
                                        // Backup: use the auth store email
                                        const authUser = useAuthStore.getState().user
                                        if (authUser?.email) store.deleteUser(authUser.email)
                                    }
                                    
                                    // Wipe local simulated stores so a new account doesn't inherit them
                                    useNetworkStore.getState().clearAll()
                                    useResponseStore.getState().clearAll()
                                    
                                    useAuthStore.getState().clearAuth()
                                    router.push('/auth')
                                }
                            }}
                            className="w-full flex items-center justify-center gap-2 py-3 border border-red-200/50 text-red-500 rounded-xl text-sm font-bold uppercase tracking-widest hover:bg-red-500/10 transition-colors"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                            Delete Account
                        </button>
                    </div>
                </main>

                {/* Right Sidebar - Social Links */}
                <aside className="hidden xl:block w-[320px] p-8 space-y-8">
                    <div className="bg-white border border-border p-6 rounded-2xl shadow-sm">
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="font-display text-xl">Social Nodes</h3>
                            {!isEditingSocial ? (
                                <button
                                    onClick={() => { setSocialForm({ linkedinUrl, twitterUrl, githubUrl }); setIsEditingSocial(true); }}
                                    className="p-1.5 rounded-lg hover:bg-surface-2 text-text-muted hover:text-primary transition-all"
                                    title="Edit social links"
                                >
                                    <Edit3 className="w-4 h-4" />
                                </button>
                            ) : (
                                <div className="flex gap-2">
                                    <button onClick={handleSaveSocial} className="p-1.5 rounded-lg bg-primary text-white hover:opacity-90 transition-all" title="Save">
                                        <Check className="w-4 h-4" />
                                    </button>
                                    <button onClick={handleCancelSocial} className="p-1.5 rounded-lg border border-border hover:bg-surface-2 transition-all" title="Cancel">
                                        <X className="w-4 h-4" />
                                    </button>
                                </div>
                            )}
                        </div>

                        {isEditingSocial ? (
                            <div className="space-y-4">
                                {socialError && (
                                    <div className="p-2 bg-red-50 border border-red-100 rounded text-[10px] text-red-500 font-bold uppercase tracking-tight">
                                        {socialError}
                                    </div>
                                )}
                                <div>
                                    <label className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1 block">LinkedIn URL</label>
                                    <div className="relative">
                                        <Linkedin className="absolute left-2 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
                                        <input
                                            value={socialForm.linkedinUrl}
                                            onChange={(e) => { setSocialForm({ ...socialForm, linkedinUrl: e.target.value }); if (socialError) setSocialError(''); }}
                                            placeholder="https://linkedin.com/in/yourprofile"
                                            className="w-full bg-surface-1 border border-border p-2 pl-8 rounded-lg text-xs focus:ring-1 focus:ring-primary outline-none text-black"
                                        />
                                    </div>
                                </div>
                                <div>
                                    <label className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1 block">Twitter (X) URL</label>
                                    <div className="relative">
                                        <Twitter className="absolute left-2 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
                                        <input
                                            value={socialForm.twitterUrl}
                                            onChange={(e) => { setSocialForm({ ...socialForm, twitterUrl: e.target.value }); if (socialError) setSocialError(''); }}
                                            placeholder="https://twitter.com/yourhandle"
                                            className="w-full bg-surface-1 border border-border p-2 pl-8 rounded-lg text-xs focus:ring-1 focus:ring-primary outline-none text-black"
                                        />
                                    </div>
                                </div>
                                <div>
                                    <label className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1 block">GitHub URL</label>
                                    <div className="relative">
                                        <Github className="absolute left-2 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
                                        <input
                                            value={socialForm.githubUrl}
                                            onChange={(e) => { setSocialForm({ ...socialForm, githubUrl: e.target.value }); if (socialError) setSocialError(''); }}
                                            placeholder="https://github.com/yourusername"
                                            className="w-full bg-surface-1 border border-border p-2 pl-8 rounded-lg text-xs focus:ring-1 focus:ring-primary outline-none text-black"
                                        />
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="space-y-6">
                                {linkedinUrl ? (
                                    <Link href={formatURL(linkedinUrl)} target="_blank" className="flex items-center gap-4 group">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border group-hover:bg-primary group-hover:text-white transition-all text-black">
                                            <Linkedin className="w-5 h-5" />
                                        </div>
                                        <div className="overflow-hidden">
                                            <p className="text-[10px] font-bold uppercase text-text-muted">LinkedIn</p>
                                            <p className="text-sm font-bold truncate text-black">{extractHandle(linkedinUrl, '')}</p>
                                        </div>
                                    </Link>
                                ) : (
                                    <div className="flex items-center gap-4 opacity-40 text-black">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border"><Linkedin className="w-5 h-5" /></div>
                                        <p className="text-xs italic">No LinkedIn added</p>
                                    </div>
                                )}
                                {twitterUrl ? (
                                    <Link href={formatURL(twitterUrl.startsWith('http') ? twitterUrl : `twitter.com/${twitterUrl.replace('@', '')}`)} target="_blank" className="flex items-center gap-4 group">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border group-hover:bg-black group-hover:text-white transition-all text-black">
                                            <Twitter className="w-5 h-5" />
                                        </div>
                                        <div className="overflow-hidden">
                                            <p className="text-[10px] font-bold uppercase text-text-muted">Twitter</p>
                                            <p className="text-sm font-bold truncate text-black">{twitterUrl.startsWith('http') ? extractHandle(twitterUrl) : (twitterUrl.startsWith('@') ? twitterUrl : `@${twitterUrl}`)}</p>
                                        </div>
                                    </Link>
                                ) : (
                                    <div className="flex items-center gap-4 opacity-40 text-black">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border"><Twitter className="w-5 h-5" /></div>
                                        <p className="text-xs italic">No Twitter added</p>
                                    </div>
                                )}
                                {githubUrl ? (
                                    <Link href={formatURL(githubUrl)} target="_blank" className="flex items-center gap-4 group">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border group-hover:bg-black group-hover:text-white transition-all text-black">
                                            <Github className="w-5 h-5" />
                                        </div>
                                        <div className="overflow-hidden">
                                            <p className="text-[10px] font-bold uppercase text-text-muted">GitHub</p>
                                            <p className="text-sm font-bold truncate text-black">{extractHandle(githubUrl, '')}</p>
                                        </div>
                                    </Link>
                                ) : (
                                    <div className="flex items-center gap-4 opacity-40 text-black">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border"><Github className="w-5 h-5" /></div>
                                        <p className="text-xs italic">No GitHub added</p>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Upgrade CTA */}
                    <div className="bg-primary p-6 rounded-2xl text-white shadow-xl relative overflow-hidden group">
                        <div className="relative z-10">
                            <h3 className="font-display text-xl mb-2">
                                {subscription === 'Free' ? 'Upgrade Your Plan' : 'Manage Your Plan'}
                            </h3>
                            <p className="text-white/70 text-sm mb-6 leading-relaxed">
                                {subscription === 'Free'
                                    ? 'Unlock verified badges, unlimited signals, and AI market intelligence.'
                                    : 'View your current benefits, usage, and manage your subscription.'}
                            </p>
                            <button
                                onClick={() => router.push('/subscription')}
                                className="w-full bg-white text-black py-3 rounded-xl font-bold uppercase tracking-widest text-[10px] hover:bg-surface-2 transition-colors"
                            >
                                {subscription === 'Free' ? 'Upgrade Now →' : 'View Plan →'}
                            </button>
                        </div>
                        <Signal className="absolute -bottom-10 -right-10 w-48 h-48 opacity-10 rotate-12 group-hover:rotate-45 transition-transform duration-1000" />
                    </div>

                    {/* Sponsored Card */}
                    <div className="bg-white border border-border p-6 rounded-2xl shadow-sm">
                        <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-text-muted mb-3">Sponsored</p>
                        <h3 className="font-display text-lg mb-2 leading-snug">Want to run ads here?</h3>
                        <p className="text-sm text-text-secondary leading-relaxed mb-5">Reach 1000s of founders, investors &amp; mentors in our ecosystem.</p>
                        <button
                            onClick={() => router.push('/subscription')}
                            className="w-full bg-black text-white py-3 rounded-2xl font-bold uppercase tracking-widest text-[10px] hover:bg-primary transition-colors"
                        >
                            Get started →
                        </button>
                    </div>
                </aside>
                <MobileBottomNav />
            </div>
        </div>
    )
}
