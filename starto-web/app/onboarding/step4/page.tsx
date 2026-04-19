"use client"

import { useState } from 'react'
import { motion } from 'framer-motion'
import { Navigation as NavIcon, ArrowRight } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useOnboardingStore } from '@/store/useOnboardingStore'
import CityAutocomplete from '@/components/CityAutocomplete'

export default function OnboardingStep4() {
    const router = useRouter()
    const { city, lat, lng, setLocation } = useOnboardingStore()
    const [loading, setLoading] = useState(false)

    const handleContinue = () => {
        if (city && lat && lng) {
            router.push('/onboarding/step5')
        }
    }

    const handleLocationChange = (cityName: string, latitude?: number, longitude?: number) => {
        if (latitude && longitude) {
            setLocation(cityName, latitude, longitude)
        } else {
            // If only name is provided (typing), we reset coordinates in store
            // but keep the name. The store's setLocation requires lat/lng though.
            // We'll manage local state if needed or just wait for selection.
        }
    }

    const handleUseCurrentLocation = () => {
        if ("geolocation" in navigator) {
            setLoading(true)
            navigator.geolocation.getCurrentPosition(
                async (position) => {
                    const { latitude, longitude } = position.coords;
                    try {
                        const response = await fetch(`https://maps.googleapis.com/maps/api/geocode/json?latlng=${latitude},${longitude}&key=${process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY}`);
                        const data = await response.json();
                        if (data.results && data.results[0]) {
                            const addressComponents = data.results[0].address_components;
                            const cityComponent = addressComponents.find((c: any) => c.types.includes("locality") || c.types.includes("administrative_area_level_2"));
                            const cityName = cityComponent ? cityComponent.long_name : "Unknown City";
                            setLocation(cityName, latitude, longitude);
                        }
                    } catch (error) {
                        console.error("Geocoding failed", error);
                        setLocation("Selected Location", latitude, longitude);
                    }
                    setLoading(false);
                },
                (error) => {
                    console.error("Geolocation error", error);
                    setLoading(false);
                }
            );
        }
    }

    return (
        <div className="min-h-screen bg-[#0A0A0B] flex items-center justify-center p-6 font-sans">
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-[#161618] p-10 rounded-[2rem] border border-white/5 max-w-lg w-full shadow-2xl relative overflow-hidden"
            >
                {/* Background Glow */}
                <div className="absolute -top-24 -right-24 w-48 h-48 bg-primary/10 blur-[100px] rounded-full" />
                
                <div className="relative z-10">
                    <div className="mb-8">
                        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-primary text-[10px] uppercase font-bold tracking-widest mb-4">
                            Step 04 / 05
                        </div>
                        <h2 className="text-3xl font-display text-white mb-3">Where are you based?</h2>
                        <p className="text-gray-400 text-sm leading-relaxed">
                            This helps us show you nearby ecosystem nodes, founders, and investors in your immediate vicinity.
                        </p>
                    </div>

                    <div className="space-y-6 mb-10">
                        <div className="relative group">
                            <CityAutocomplete 
                                value={city} 
                                onChange={handleLocationChange} 
                            />
                        </div>

                        <button 
                            onClick={handleUseCurrentLocation}
                            disabled={loading}
                            className="flex items-center gap-2 text-primary text-xs font-bold uppercase tracking-widest hover:text-white transition-colors group disabled:opacity-50"
                        >
                            <NavIcon className={`w-3.5 h-3.5 group-hover:rotate-45 transition-transform ${loading ? 'animate-spin' : ''}`} />
                            {loading ? 'Detecting...' : 'Use my current location'}
                        </button>
                    </div>

                    <div className="bg-white/5 border border-white/5 p-4 rounded-xl mb-10 backdrop-blur-sm">
                        <p className="text-[11px] text-gray-400 leading-relaxed italic">
                            "Starto uses your precise location to bridge the gap between digital networking and real-world collaboration."
                        </p>
                    </div>

                    <button
                        onClick={handleContinue}
                        disabled={!city || !lat || !lng}
                        className={`w-full py-4 rounded-xl font-bold uppercase tracking-[0.2em] text-xs transition-all flex items-center justify-center gap-3 shadow-xl ${
                            city && lat && lng 
                            ? 'bg-white text-black hover:scale-[1.02] active:scale-[0.98]' 
                            : 'bg-white/5 text-gray-600 cursor-not-allowed'
                        }`}
                    >
                        Continue
                        <ArrowRight className="w-4 h-4" />
                    </button>
                </div>
            </motion.div>
        </div>
    )
}
