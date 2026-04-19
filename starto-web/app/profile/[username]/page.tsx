"use client"

import Sidebar from '@/components/feed/Sidebar'
import { MapPin, Globe, Twitter, Linkedin, Github, Zap, Users, BadgeCheck, Star } from 'lucide-react'
import Image from 'next/image'
import { useState, useEffect } from 'react'
import { useAuthStore } from '@/store/useAuthStore'
import { useSignalStore } from '@/store/useSignalStore'
import { useNetworkStore } from '@/store/useNetworkStore'
import { useRatingStore } from '@/store/useRatingStore'
import Link from 'next/link'
import { usersApi } from '@/lib/apiClient'
import { motion } from 'framer-motion'

export default function PublicProfile({ params }: { params: { username: string } }) {
    const { username: paramUsername } = params

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

    const { user: currentUser, isAuthenticated, token } = useAuthStore()
    const storeUsername = currentUser?.username || ''
    
    const [fetchedUser, setFetchedUser] = useState<any | null>(null)
    const [isLoadingUser, setIsLoadingUser] = useState(true)

    const { signals } = useSignalStore()
    const { connections, sentRequests, sendRequest, fetchRequests } = useNetworkStore()
    const { addRating, getAverageRating, getRatingsFor, hasRated } = useRatingStore()

    const [isMounted, setIsMounted] = useState(false)
    const [showAllSignals, setShowAllSignals] = useState(false)
    const [requestJustSent, setRequestJustSent] = useState(false)
    
    useEffect(() => {
        setIsMounted(true)
        setIsLoadingUser(true)
        usersApi.getByUsername(paramUsername).then(({ data, error }) => {
            if (data) {
                setFetchedUser(data)
            }
            setIsLoadingUser(false)
        })
    }, [paramUsername])

    const isOwnProfile = isAuthenticated && paramUsername === storeUsername
    
    // Use fetched data if available, fallback to store if own profile, else defaults
    const activeUser = isOwnProfile ? currentUser : fetchedUser
    
    const {
        isVerified = false, 
        subscription = 'Free', 
        name = '',
        role = '', 
        city = '', 
        bio = '', 
        websiteUrl = '', 
        linkedinUrl = '', 
        twitterUrl = '', 
        githubUrl = '', 
        avatarUrl = null
    } = (activeUser as any) || {}

    const userSignals = signals.filter(s => s.username === paramUsername)

    const displayName = name || paramUsername.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
    const displayRole = role || 'Member'
    const displayCity = city || 'India'
    const displayBio = bio || `Active member of the Starto ecosystem. Raising signals in the ${displayRole} space.`
    const displayAvatarUrl = avatarUrl
    const displayWebsite = websiteUrl
    const displayLinkedin = linkedinUrl
    const displayTwitter = twitterUrl
    const displayGithub = githubUrl
    const displayVerified = isVerified
    const displaySubscription = subscription === 'pro' ? 'Pro' : 'Free'

    // Rating data
    const avgRating = getAverageRating(paramUsername)
    const allRatings = getRatingsFor(paramUsername)
    const alreadyRated = hasRated(storeUsername, paramUsername)
    const [hoverStar, setHoverStar] = useState(0)
    const [selectedStar, setSelectedStar] = useState(0)
    const [ratingComment, setRatingComment] = useState('')
    const [ratingSubmitted, setRatingSubmitted] = useState(false)

    // Rating distribution (Play Store style)
    const ratingDistribution = [5, 4, 3, 2, 1].map(star => ({
        star,
        count: allRatings.filter(r => r.stars === star).length,
        pct: allRatings.length > 0 ? Math.round((allRatings.filter(r => r.stars === star).length / allRatings.length) * 100) : 0
    }))

    const handleSubmitRating = () => {
        if (!selectedStar || isOwnProfile || alreadyRated) return
        addRating({ fromUsername: storeUsername, toUsername: paramUsername, stars: selectedStar, comment: ratingComment })
        setRatingSubmitted(true)
    }

    // Connections count for this user (only shown publicly if it's own profile)
    const connectionsCount = isMounted && (isOwnProfile ? (connections?.length || 0) : (Array.isArray(connections) ? connections.filter(c => c.username === paramUsername).length : 0))

    return (
        <div className="min-h-screen bg-background flex justify-center">
            <div className="max-w-[1400px] w-full flex">
                <Sidebar />

                <main className="flex-1 max-w-[680px] border-r border-border min-h-screen p-0">
                    {/* Profile Header (No Banner) */}
                    <div className="pt-8 px-8 flex items-end gap-6 border-b border-border pb-8 bg-surface-1">
                        <div className="w-32 h-32 bg-white rounded-3xl p-1.5 border-4 border-background shadow-2xl relative overflow-hidden shrink-0">
                            {displayAvatarUrl ? (
                                <Image src={displayAvatarUrl} alt="Profile" fill className="object-cover rounded-2xl" unoptimized />
                            ) : (
                                <Image src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(paramUsername)}`} alt={displayName} fill className="object-cover rounded-2xl" unoptimized />
                            )}
                        </div>
                        
                        <div className="flex-1 pb-2">
                            <div className="flex items-center gap-2 mb-1">
                                <h1 className="text-3xl font-display text-black">{displayName}</h1>
                                {(displayVerified || displaySubscription === 'Pro' || displaySubscription === 'Founder') && (
                                    <span title={`${displaySubscription} Verified`} className="relative inline-flex items-center justify-center">
                                        <BadgeCheck className="w-7 h-7 fill-black text-white" />
                                    </span>
                                )}
                            </div>
                            <p className="text-text-secondary font-medium flex items-center gap-2">
                                {displayRole} • {displayCity.split(',')[0]}
                                <span className="w-1.5 h-1.5 rounded-full bg-accent-green" />
                                <span className="text-[10px] px-2 py-0.5 bg-surface-2 rounded-full uppercase tracking-tighter font-bold border border-border text-black">
                                    {displaySubscription} Account
                                </span>
                            </p>
                        </div>

                        {!isOwnProfile && (() => {
                            const isPending = isMounted && Array.isArray(sentRequests) && sentRequests.some(r => r.username === paramUsername && r.status === 'pending');
                            return (
                                <div className="pb-2">
                                    <button 
                                        disabled={!isMounted || isPending || userSignals.length === 0}
                                        onClick={async () => {
                                            if (!token) {
                                                alert('Please login to connect');
                                                return;
                                            }
                                            if (userSignals.length === 0) {
                                                alert('This user has no active signals to connect to.');
                                                return;
                                            }
                                            try {
                                                await sendRequest(userSignals[0].id, 'Hi, I saw your profile and wanted to connect!', token || 'mock-token', paramUsername);
                                                setRequestJustSent(true);
                                                // Animation state handles rendering "Sending..." then relies on isPending returning true
                                            } catch (err) {
                                                alert('Failed to send request.');
                                            }
                                        }}
                                        className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-widest transition-all shadow-lg active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed ${
                                            requestJustSent || isPending ? 'bg-green-500 text-white' : 'bg-black text-white hover:bg-black/80'
                                        }`}
                                    >
                                        {requestJustSent ? (
                                            <motion.div initial={{ scale: 0.5, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="flex items-center gap-2">
                                                <BadgeCheck className="w-4 h-4" /> Sending...
                                            </motion.div>
                                        ) : isPending ? (
                                            <>
                                                <BadgeCheck className="w-4 h-4" /> Request Sent
                                            </>
                                        ) : (
                                            <>
                                                <Users className="w-4 h-4" /> Connect
                                            </>
                                        )}
                                    </button>
                                </div>
                            );
                        })()}
                    </div>

                    {/* Profile Body */}
                    <div className="px-8 pt-8 pb-8 border-b border-border">
                        <div className="flex justify-between items-start mb-6">
                            <div className="flex-1 mr-4">
                                <div className="flex gap-4 mb-6">
                                    <span className="flex items-center gap-1.5 text-xs text-text-secondary">
                                        <MapPin className="w-3.5 h-3.5" /> {displayCity.split(',')[0]}
                                    </span>
                                    {displayWebsite && (
                                        <Link href={formatURL(displayWebsite)} target="_blank" className="flex items-center gap-1.5 text-xs text-primary hover:underline">
                                            <Globe className="w-3.5 h-3.5" /> {displayWebsite.replace(/^https?:\/\//, '')}
                                        </Link>
                                    )}
                                </div>
                                <p className="text-sm text-text-secondary leading-relaxed max-w-lg mb-6">{displayBio}</p>
                            </div>
                        </div>

                        {/* Public Stats — Signals, Connections, Rating */}
                        <div className="flex gap-12 pt-6 mb-8">
                            <div>
                                <p className="text-[10px] uppercase font-bold text-text-muted mb-1">Signals</p>
                                <p className="text-2xl font-mono font-bold text-black">{userSignals.length}</p>
                            </div>
                            <div>
                                <p className="text-[10px] uppercase font-bold text-text-muted mb-1">Connections</p>
                                <p className="text-2xl font-mono font-bold text-black">{isMounted && isOwnProfile ? connections.length : (fetchedUser?.connectionCount || '—')}</p>
                            </div>
                            <div>
                                <p className="text-[10px] uppercase font-bold text-text-muted mb-1">Rating</p>
                                <div className="flex items-center gap-1">
                                    <p className="text-2xl font-mono font-bold text-black">{(isMounted && avgRating > 0) ? avgRating.toFixed(1) : '—'}</p>
                                    {isMounted && avgRating > 0 && <Star className="w-5 h-5 fill-yellow-400 text-yellow-400" />}
                                </div>
                                <p className="text-[10px] text-text-muted">{allRatings.length} {allRatings.length === 1 ? 'review' : 'reviews'}</p>
                            </div>
                        </div>

                        {/* Social Nodes (Moved to Main Body) */}
                        {(displayLinkedin || displayTwitter || displayGithub) && (
                            <div className="flex flex-wrap gap-4 pt-6 border-t border-border mt-4">
                                {displayLinkedin && (
                                    <Link href={formatURL(displayLinkedin)} target="_blank" className="flex items-center gap-2 group bg-surface-2 px-3 py-2 rounded-xl border border-border hover:border-primary transition-all">
                                        <Linkedin className="w-4 h-4 text-black group-hover:text-primary" />
                                        <span className="text-xs font-bold text-black">{extractHandle(displayLinkedin, '')}</span>
                                    </Link>
                                )}
                                {displayTwitter && (
                                    <Link href={formatURL(displayTwitter.startsWith('http') ? displayTwitter : `twitter.com/${displayTwitter.replace('@', '')}`)} target="_blank" className="flex items-center gap-2 group bg-surface-2 px-3 py-2 rounded-xl border border-border hover:border-black transition-all">
                                        <Twitter className="w-4 h-4 text-black group-hover:text-black" />
                                        <span className="text-xs font-bold text-black">{displayTwitter.startsWith('http') ? extractHandle(displayTwitter) : (displayTwitter.startsWith('@') ? displayTwitter : `@${displayTwitter}`)}</span>
                                    </Link>
                                )}
                                {displayGithub && (
                                    <Link href={formatURL(displayGithub)} target="_blank" className="flex items-center gap-2 group bg-surface-2 px-3 py-2 rounded-xl border border-border hover:border-black transition-all">
                                        <Github className="w-4 h-4 text-black group-hover:text-black" />
                                        <span className="text-xs font-bold text-black">{extractHandle(displayGithub, '')}</span>
                                    </Link>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Signals Feed */}
                    <div className="p-8 border-b border-border">
                        <div className="flex items-center gap-8 border-b border-border mb-8">
                            <button className="pb-4 border-b-2 border-primary font-bold text-xs uppercase tracking-widest">Active Signals</button>
                        </div>
                        <div className="space-y-6">
                            {userSignals.length === 0 ? (
                                <div className="flex flex-col items-center py-12 text-center text-text-muted">
                                    <Users className="w-10 h-10 mb-3 opacity-30" />
                                    <p className="text-sm">No active signals yet</p>
                                </div>
                            ) : (
                                <>
                                    {(showAllSignals ? userSignals : userSignals.slice(0, 3)).map(signal => (
                                        <div key={signal.id} className="p-6 bg-surface-2 rounded-2xl border border-border group hover:border-primary transition-all">
                                            <div className="flex justify-between items-start mb-4">
                                                <span className="text-[10px] px-2 py-0.5 bg-black text-white rounded-full uppercase font-bold tracking-widest">{signal.category}</span>
                                                <span className="text-[10px] font-bold text-text-muted">{signal.timeAgo}</span>
                                            </div>
                                            <h3 className="text-xl font-display mb-2 group-hover:text-primary transition-colors">{signal.title}</h3>
                                            <p className="text-sm text-text-secondary line-clamp-2">{signal.description}</p>
                                            <div className="mt-4 pt-4 border-t border-border flex justify-between items-center text-[10px] font-bold uppercase tracking-widest">
                                                <span className="flex items-center gap-1.5"><Zap className="w-3.5 h-3.5 text-primary" /> {signal.stats.responses} Responses</span>
                                            </div>
                                        </div>
                                    ))}
                                    {userSignals.length > 3 && (
                                        <button
                                            onClick={() => setShowAllSignals(!showAllSignals)}
                                            className="w-full py-3 rounded-xl border border-border text-sm font-bold hover:bg-surface-2 transition-all mt-2 text-black"
                                        >
                                            {showAllSignals ? 'View Less' : `View All ${userSignals.length} Signals`}
                                        </button>
                                    )}
                                </>
                            )}
                        </div>
                    </div>

                    {/* ★ Feedback / Rating Section */}
                    {!isOwnProfile && (
                        <div className="p-8">
                            <h2 className="font-display text-xl mb-2">Ratings & Feedback</h2>
                            <p className="text-xs text-text-secondary mb-6">Share your experience with @{paramUsername}</p>

                            {/* Rating Distribution (Play Store style) */}
                            {allRatings.length > 0 && (
                                <div className="flex gap-8 mb-8 p-5 bg-surface-2 rounded-2xl border border-border">
                                    <div className="flex flex-col items-center justify-center">
                                        <p className="text-5xl font-mono font-bold">{avgRating.toFixed(1)}</p>
                                        <div className="flex gap-0.5 my-1">
                                            {[1,2,3,4,5].map(s => (
                                                <Star key={s} className={`w-4 h-4 ${s <= Math.round(avgRating) ? 'fill-yellow-400 text-yellow-400' : 'text-border fill-border'}`} />
                                            ))}
                                        </div>
                                        <p className="text-[10px] text-text-muted">{allRatings.length} reviews</p>
                                    </div>
                                    <div className="flex-1 space-y-1.5">
                                        {ratingDistribution.map(({ star, count, pct }) => (
                                            <div key={star} className="flex items-center gap-2 text-xs">
                                                <span className="w-2 text-text-muted">{star}</span>
                                                <Star className="w-3 h-3 fill-yellow-400 text-yellow-400" />
                                                <div className="flex-1 h-2 bg-border rounded-full overflow-hidden">
                                                    <div className="h-full bg-yellow-400 rounded-full transition-all" style={{ width: `${pct}%` }} />
                                                </div>
                                                <span className="w-6 text-text-muted">{count}</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Leave a Rating */}
                            {ratingSubmitted || alreadyRated ? (
                                <div className="p-6 bg-green-50 border border-green-200 rounded-2xl text-center">
                                    <Star className="w-8 h-8 fill-yellow-400 text-yellow-400 mx-auto mb-2" />
                                    <p className="font-bold text-green-700">Thanks for your feedback!</p>
                                    <p className="text-sm text-green-600">Your rating has been submitted.</p>
                                </div>
                            ) : (
                                <div className="p-6 bg-white border border-border rounded-2xl">
                                    <p className="font-bold text-sm mb-4 uppercase tracking-widest text-[10px] text-text-muted">Rate this member</p>
                                    <div className="flex gap-2 mb-4">
                                        {[1, 2, 3, 4, 5].map(star => (
                                            <button
                                                key={star}
                                                onMouseEnter={() => setHoverStar(star)}
                                                onMouseLeave={() => setHoverStar(0)}
                                                onClick={() => setSelectedStar(star)}
                                                className="transition-transform hover:scale-110"
                                            >
                                                <Star className={`w-9 h-9 transition-all ${
                                                    star <= (hoverStar || selectedStar)
                                                        ? 'fill-yellow-400 text-yellow-400'
                                                        : 'fill-border text-border'
                                                }`} />
                                            </button>
                                        ))}
                                        {selectedStar > 0 && (
                                            <span className="ml-2 text-sm font-medium self-center text-text-secondary">
                                                {['', 'Poor', 'Fair', 'Good', 'Very Good', 'Excellent'][selectedStar]}
                                            </span>
                                        )}
                                    </div>
                                    <textarea
                                        placeholder="Write a short review (optional)..."
                                        value={ratingComment}
                                        onChange={e => setRatingComment(e.target.value)}
                                        rows={3}
                                        className="w-full border border-border rounded-xl p-3 text-sm resize-none outline-none focus:border-primary mb-4"
                                    />
                                    <button
                                        disabled={!selectedStar}
                                        onClick={handleSubmitRating}
                                        className="px-6 py-2.5 bg-black text-white text-xs font-bold uppercase tracking-widest rounded-xl hover:opacity-90 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
                                    >
                                        Submit Rating
                                    </button>
                                </div>
                            )}
                        </div>
                    )}
                </main>

                {/* Right Sidebar — Social Links */}
                <aside className="hidden xl:block w-[320px] p-8 space-y-8">
                    {(displayLinkedin || displayTwitter || displayGithub) && (
                        <div className="bg-white border border-border p-6 rounded-2xl shadow-sm">
                            <h3 className="font-display text-xl mb-6">Social Nodes</h3>
                            <div className="space-y-6">
                                {displayLinkedin && (
                                    <Link href={formatURL(displayLinkedin)} target="_blank" className="flex items-center gap-4 group">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border group-hover:bg-primary group-hover:text-white transition-all text-black">
                                            <Linkedin className="w-5 h-5" />
                                        </div>
                                        <div>
                                            <p className="text-[10px] font-bold uppercase text-text-muted">LinkedIn</p>
                                            <p className="text-sm font-medium truncate text-black">{extractHandle(displayLinkedin, '')}</p>
                                        </div>
                                    </Link>
                                )}
                                {displayTwitter && (
                                    <Link href={formatURL(`twitter.com/${displayTwitter.replace('@', '')}`)} target="_blank" className="flex items-center gap-4 group">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border group-hover:bg-black group-hover:text-white transition-all">
                                            <Twitter className="w-5 h-5" />
                                        </div>
                                        <div>
                                            <p className="text-[10px] font-bold uppercase text-text-muted">Twitter</p>
                                            <p className="text-sm font-medium text-black">{displayTwitter.startsWith('http') ? extractHandle(displayTwitter) : (displayTwitter.startsWith('@') ? displayTwitter : `@${displayTwitter}`)}</p>
                                        </div>
                                    </Link>
                                )}
                                {displayGithub && (
                                    <Link href={formatURL(displayGithub)} target="_blank" className="flex items-center gap-4 group">
                                        <div className="w-10 h-10 bg-surface-2 rounded-xl flex items-center justify-center border border-border group-hover:bg-black group-hover:text-white transition-all text-black">
                                            <Github className="w-5 h-5" />
                                        </div>
                                        <div>
                                            <p className="text-[10px] font-bold uppercase text-text-muted">GitHub</p>
                                            <p className="text-sm font-medium truncate text-black">{extractHandle(displayGithub, '')}</p>
                                        </div>
                                    </Link>
                                )}
                            </div>
                        </div>
                    )}
                </aside>
            </div>
        </div>
    )
}
