"use client";

import React, { useState, useEffect, useRef } from "react";
import { Search, MapPin } from "lucide-react";

interface CityAutocompleteProps {
    value: string;
    onChange: (city: string, lat?: number, lng?: number) => void;
}

export default function CityAutocomplete({ value, onChange }: CityAutocompleteProps) {
    const [query, setQuery] = useState(value);
    const [suggestions, setSuggestions] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [isOpen, setIsOpen] = useState(false);
    const [isApiReady, setIsApiReady] = useState(false);
    const wrapperRef = useRef<HTMLDivElement>(null);
    const autocompleteService = useRef<any>(null);
    const placesService = useRef<any>(null);

    // Monitor for Google Maps API readiness
    useEffect(() => {
        const checkApi = () => {
            if (typeof window !== "undefined" && (window as any).google?.maps?.places) {
                setIsApiReady(true);
                return true;
            }
            return false;
        };

        if (checkApi()) return;

        const interval = setInterval(() => {
            if (checkApi()) clearInterval(interval);
        }, 500);

        return () => clearInterval(interval);
    }, []);

    // Sync with parent value
    useEffect(() => {
        if (value !== query && (value || query)) {
            setQuery(value || "");
        }
    }, [value]);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    useEffect(() => {
        const timer = setTimeout(() => {
            // Only search if query is 2+ chars and doesn't match current selection
            if (query && query.length > 1 && query !== value && isOpen && isApiReady) {
                fetchCities(query);
            } else {
                setSuggestions([]);
            }
        }, 300);

        return () => clearTimeout(timer);
    }, [query, value, isOpen, isApiReady]);

    const fetchCities = (searchQuery: string) => {
        setLoading(true);
        if (!autocompleteService.current && typeof window !== "undefined" && (window as any).google?.maps?.places) {
            autocompleteService.current = new (window as any).google.maps.places.AutocompleteService();
        }

        if (autocompleteService.current) {
            console.log(`Searching for cities: ${searchQuery}`);
            autocompleteService.current.getPlacePredictions(
                { 
                    input: searchQuery, 
                    // Relaxed types to ensure results in all regions
                    types: ['(regions)'] 
                },
                (predictions: any[], status: string) => {
                    setLoading(false);
                    console.log(`Autocomplete status for "${searchQuery}":`, status);
                    
                    if (status === (window as any).google.maps.places.PlacesServiceStatus.OK && predictions) {
                        setSuggestions(predictions);
                    } else {
                        if (status === "ZERO_RESULTS") {
                            // Try one more time without type restrictions if zero results
                            autocompleteService.current.getPlacePredictions(
                                { input: searchQuery },
                                (fallbackPredictions: any[], fallbackStatus: string) => {
                                    if (fallbackStatus === "OK" && fallbackPredictions) {
                                        setSuggestions(fallbackPredictions);
                                    } else {
                                        setSuggestions([]);
                                    }
                                }
                            );
                        } else {
                            setSuggestions([]);
                        }
                    }
                }
            );
        } else {
            setLoading(false);
            console.warn("AutocompleteService not initialized yet.");
        }
    };

    const handleSelect = (item: any) => {
        const cityName = item.description;
        setQuery(cityName);
        setIsOpen(false);

        // Fetch lat/lng details
        if (typeof window !== "undefined" && (window as any).google?.maps?.places) {
            if (!placesService.current) {
                const element = document.createElement('div');
                placesService.current = new (window as any).google.maps.places.PlacesService(element);
            }

            placesService.current.getDetails(
                { placeId: item.place_id, fields: ['geometry'] },
                (place: any, status: string) => {
                    if (status === (window as any).google.maps.places.PlacesServiceStatus.OK && place.geometry) {
                        const lat = place.geometry.location.lat();
                        const lng = place.geometry.location.lng();
                        onChange(cityName, lat, lng);
                    } else {
                        onChange(cityName);
                    }
                }
            );
        } else {
            onChange(cityName);
        }
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value;
        setQuery(val);
        setIsOpen(true);
        // On typing, we only update name, clearing coordinates
        onChange(val);
    };

    return (
        <div ref={wrapperRef} className="relative w-full">
            <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 opacity-50" />
                <input
                    type="text"
                    value={query}
                    onChange={handleInputChange}
                    onFocus={() => setIsOpen(true)}
                    placeholder="Search for a city..."
                    autoComplete="off"
                    required
                    className="w-full bg-white/5 border border-white/10 rounded-lg pl-10 pr-4 py-3.5 text-white placeholder:text-gray-500 focus:outline-none focus:border-primary/50 focus:bg-white/10 transition-all text-sm"
                />
            </div>

            {isOpen && query && query.length > 2 && (
                <div className="absolute z-[100] w-full mt-2 bg-[#1A1B1E] border border-white/10 rounded-xl shadow-2xl overflow-hidden max-h-72 overflow-y-auto backdrop-blur-xl">
                    {loading ? (
                        <div className="px-5 py-4 text-xs text-gray-500 flex items-center gap-2 animate-pulse">
                            <div className="w-2 h-2 bg-primary rounded-full" /> Searching places...
                        </div>
                    ) : suggestions.length > 0 ? (
                        <ul className="py-2">
                            {suggestions.map((item, index) => (
                                <li
                                    key={index}
                                    onClick={() => handleSelect(item)}
                                    className="px-4 py-3 cursor-pointer hover:bg-white/5 flex items-center gap-4 transition-colors border-b border-white/[0.03] last:border-0 group"
                                >
                                    <div className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center group-hover:bg-primary/20 group-hover:text-primary transition-colors">
                                        <MapPin className="w-4 h-4 text-gray-500 group-hover:text-primary" />
                                    </div>
                                    <div className="flex flex-col flex-1">
                                        <span className="text-gray-200 text-sm font-medium group-hover:text-white transition-colors">
                                            {item.structured_formatting?.main_text || item.description}
                                        </span>
                                        {item.structured_formatting?.secondary_text && (
                                            <span className="text-[11px] text-gray-500">{item.structured_formatting.secondary_text}</span>
                                        )}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <div className="px-5 py-4 text-xs text-gray-500">No matching cities found.</div>
                    )}
                </div>
            )}
        </div>
    );
}

