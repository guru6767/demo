"use client"

import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, Zap, CheckCircle, AlertTriangle } from 'lucide-react'
import { useSignalStore, Signal } from '@/store/useSignalStore'
import { useAuthStore } from '@/store/useAuthStore'
import { useRouter } from 'next/navigation'
import { signalsApi } from '@/lib/apiClient'

interface RaiseSignalModalProps {
    isOpen: boolean;
    onClose: () => void;
    editSignal?: Signal;
}

export default function RaiseSignalModal({ isOpen, onClose, editSignal }: RaiseSignalModalProps) {
    const { user, token } = useAuthStore();
    const { addSignal } = useSignalStore();
    const router = useRouter();
    
    const [signalType, setSignalType] = useState<'need' | 'help'>('need');
    const [category, setCategory] = useState('Talent');
    const [headline, setHeadline] = useState('');
    const [details, setDetails] = useState('');
    const [duration, setDuration] = useState(0);
    const [submitting, setSubmitting] = useState(false);
    const [toast, setToast] = useState<{ type: 'success' | 'warn'; msg: string } | null>(null);

    useEffect(() => {
        if (isOpen && editSignal) {
            setSignalType(editSignal.type || 'need');
            setCategory(editSignal.category);
            setHeadline(editSignal.title);
            setDetails(editSignal.description);
            const parsedDuration = parseInt(editSignal.strength.split(' ')[0]);
            setDuration(isNaN(parsedDuration) ? 7 : parsedDuration);
        } else if (isOpen && !editSignal) {
            setHeadline('');
            setDetails('');
            setCategory('Talent');
            setDuration(0);
            setSignalType('need');
            setToast(null);
        }
    }, [isOpen, editSignal]);

    if (!isOpen) return null

    const handleBroadcast = async () => {
        if (!headline || !details || duration === 0) return;
        if (duration > 7) {
            onClose();
            router.push('/subscription');
            return;
        }

        setSubmitting(true);
        setToast(null);

        const currentUsername = user?.username || 'user';

        if (editSignal) {
            // Edit mode: try to update backend first
            const authToken = token || (user?.username ? `dev_${user.username}` : '');
            
            const { error } = await signalsApi.update(
                editSignal.id,
                {
                    title: headline,
                    description: details,
                    category,
                    type: signalType,
                    seeking: category,
                    timelineDays: duration,
                    signalStrength: `${duration} Days`,
                },
                authToken
            );

            if (!error) {
                // Also update local store for instant UI feedback
                useSignalStore.getState().updateSignal(editSignal.id, {
                    title: headline,
                    category,
                    description: details,
                    strength: `${duration} Days`,
                    type: signalType,
                });
                setSubmitting(false);
                onClose();
            } else {
                setToast({ type: 'warn', msg: `Server update failed: ${error}. Changes may not persist.` });
                // Still update local store so user sees change immediately
                useSignalStore.getState().updateSignal(editSignal.id, {
                    title: headline,
                    category,
                    description: details,
                    strength: `${duration} Days`,
                    type: signalType,
                });
                setTimeout(() => { onClose(); }, 2000);
            }
            setSubmitting(false);
            return;
        }

        // ── Try to post to backend first ─────────────────────────────────────
        // If we don't have a real token but have a local user, use the dev_token fallback
        const authToken = token || (user?.username ? `dev_${user.username}` : '');

        const { data, error, status } = await signalsApi.create(
            {
                title: headline,
                description: details,
                category,
                type: signalType,
                seeking: category,
                city: user?.city || '',
                timelineDays: duration,
                signalStrength: `${duration} Days`,
            },
            authToken
        );

        if (data && !error) {
            // ✅ Saved to DB — also add to local store with the SAME ID for de-duplication
            addSignal({
                title: headline,
                username: currentUsername,
                timeAgo: 'Just now',
                category,
                description: details,
                strength: `${duration} Days`,
                type: signalType,
                userPlan: user?.subscription || user?.plan || 'Free',
            }, data.id);
            setToast({ type: 'success', msg: '✓ Signal broadcast to backend DB!' });
            setTimeout(() => { onClose(); }, 1200);
        } else if (status === 401) {
            // ⚠️ Authentication Issue
            addSignal({
                title: headline,
                username: currentUsername,
                timeAgo: 'Just now',
                category,
                description: details,
                strength: `${duration} Days`,
                type: signalType,
                userPlan: user?.subscription || user?.plan || 'Free',
            });
            setToast({ type: 'warn', msg: '⚠ Session expired. Please login again.' });
            setTimeout(() => { onClose(); }, 1500);
        } else if (status === 403) {
            // 🚫 Limit reached or Forbidden
            const errMsg = error || 'Signal limit reached for your plan.';
            setToast({ type: 'warn', msg: `🚫 ${errMsg}` });
            if (errMsg.toLowerCase().includes('limit')) {
                setTimeout(() => { router.push('/subscription'); onClose(); }, 2000);
            }
        } else {
            // Network error or other — fall back to local
            addSignal({
                title: headline,
                username: currentUsername,
                timeAgo: 'Just now',
                category,
                description: details,
                strength: `${duration} Days`,
                type: signalType,
                userPlan: user?.subscription || user?.plan || 'Free',
            });
            setToast({ type: 'warn', msg: `Saved locally. Backend error: ${error}` });
            setTimeout(() => { onClose(); }, 1800);
        }

        setSubmitting(false);
    };


    return (
        <AnimatePresence>
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
                <motion.div
                    initial={{ opacity: 0, scale: 0.95, y: 20 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95, y: 20 }}
                    className="bg-white w-full max-w-[540px] rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[95vh]"
                >
                    <div className="p-6 border-b border-border flex justify-between items-center bg-white">
                        <h2 className="text-2xl font-bold font-display">{editSignal ? 'Modify Signal' : 'Raise a New Signal'}</h2>
                        <button onClick={onClose} className="p-2 hover:bg-surface-2 rounded-full transition-all text-text-muted">
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    <div className="flex-1 overflow-y-auto p-6 space-y-6">


                        {/* Category */}
                        <section>
                            <label className="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-3 block">Select Category</label>
                            <div className="flex flex-wrap gap-2">
                                {['Talent', 'Founder', 'Mentor', 'Instant Help'].map(cat => (
                                    <button 
                                        key={cat} 
                                        onClick={() => setCategory(cat)}
                                        className={`px-4 py-1.5 rounded-full border text-sm transition-all ${category === cat ? 'bg-black text-white border-black font-medium text-sm' : 'border-border text-text-secondary hover:border-black text-sm hover:text-black'}`}
                                    >
                                        {cat}
                                    </button>
                                ))}
                            </div>
                        </section>

                        {/* Headline */}
                        <section>
                            <label className="text-sm font-medium text-black mb-2 block">What's the signal headline?</label>
                            <input
                                type="text"
                                placeholder="Type a catchy headline..."
                                className="w-full bg-white p-3 rounded-none border-b border-border outline-none focus:border-black text-base placeholder:text-text-muted/60 font-serif pb-2 placeholder:font-serif"
                                value={headline}
                                onChange={(e) => setHeadline(e.target.value)}
                            />
                        </section>

                        {/* Details */}
                        <section>
                            <label className="text-sm font-medium text-black mb-2 block">Details & Context</label>
                            <textarea
                                placeholder="Explain what you are looking for or offering in more detail..."
                                className="w-full bg-white p-4 rounded-xl border border-border outline-none focus:border-black text-sm gap-2 h-32 resize-none placeholder:text-text-muted/60 shadow-sm"
                                value={details}
                                onChange={(e) => setDetails(e.target.value)}
                            />
                        </section>

                        {/* Duration */}
                        <section>
                            <label className="text-sm font-medium text-black mb-2 flex justify-between items-center block">
                                <span>Signal Duration <span className="text-red-500">*</span></span>
                                <span className={`font-mono font-bold ${duration === 0 ? 'text-text-muted' : duration > 7 ? 'text-orange-600' : 'text-primary'}`}>
                                    {duration === 0 ? 'Not Selected' : `${duration} Days`}
                                </span>
                            </label>
                            <div className={`p-4 rounded-xl border transition-colors ${duration > 7 ? 'bg-orange-50/50 border-orange-200' : duration === 0 ? 'bg-surface-2 border-red-200' : 'bg-surface-2 border-border'}`}>
                                <input 
                                    type="range" 
                                    min="0" 
                                    max="30" 
                                    className="w-full accent-black cursor-pointer"
                                    value={duration}
                                    onChange={(e) => setDuration(parseInt(e.target.value))}
                                />
                                <div className="flex justify-between text-[10px] text-text-muted mt-2 font-medium uppercase tracking-widest">
                                    <span>0</span>
                                    <span>1 Day</span>
                                    <span className="text-black font-bold">7 Days (Free Max)</span>
                                    <span>30 Days</span>
                                </div>
                            </div>
                            
                            <AnimatePresence>
                                {duration > 7 && (
                                    <motion.div 
                                        initial={{ opacity: 0, height: 0 }}
                                        animate={{ opacity: 1, height: 'auto' }}
                                        exit={{ opacity: 0, height: 0 }}
                                        className="mt-4 bg-orange-50 border border-orange-200 p-3 rounded-lg flex items-start gap-3 overflow-hidden"
                                    >
                                        <div className="bg-orange-100 p-1.5 rounded-full mt-0.5">
                                            <Zap className="w-3.5 h-3.5 text-orange-600 fill-orange-600" />
                                        </div>
                                        <div>
                                            <h4 className="text-xs font-bold text-orange-900">StarPro Feature</h4>
                                            <p className="text-[10px] text-orange-800 mt-0.5 leading-relaxed">Signals lasting longer than 7 days require an active StarPro subscription. Please upgrade to broadcast for {duration} days.</p>
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </section>
                    </div>

                    <div className="p-6 space-y-3">
                        {/* Toast feedback */}
                        <AnimatePresence>
                            {toast && (
                                <motion.div
                                    initial={{ opacity: 0, y: 8 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    exit={{ opacity: 0 }}
                                    className={`flex items-center gap-2 p-3 rounded-lg text-xs font-medium ${
                                        toast.type === 'success'
                                            ? 'bg-green-50 border border-green-200 text-green-700'
                                            : 'bg-orange-50 border border-orange-200 text-orange-700'
                                    }`}
                                >
                                    {toast.type === 'success'
                                        ? <CheckCircle className="w-4 h-4 shrink-0" />
                                        : <AlertTriangle className="w-4 h-4 shrink-0" />
                                    }
                                    {toast.msg}
                                </motion.div>
                            )}
                        </AnimatePresence>

                        <button 
                            onClick={handleBroadcast}
                            disabled={!headline || !details || duration === 0 || submitting}
                            className={`w-full text-white px-8 py-3.5 rounded-xl font-bold text-xs tracking-wider flex items-center justify-center gap-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed uppercase ${duration > 7 ? 'bg-orange-500 hover:bg-orange-600' : 'bg-black hover:bg-black/90'}`}
                        >
                            {submitting ? (
                                <><Zap className="w-4 h-4 fill-white text-white animate-pulse" /> Broadcasting…</>
                            ) : duration === 0 ? (
                                <>Select Duration to Broadcast</>
                            ) : duration > 7 ? (
                                <><Zap className="w-4 h-4 fill-white text-white" /> Upgrade to Pro → Subscription</>
                            ) : (
                                <><Zap className="w-4 h-4 fill-white text-white" /> Broadcast to Network</>
                            )}
                        </button>
                    </div>
                </motion.div>
            </div>
        </AnimatePresence>
    )
}
