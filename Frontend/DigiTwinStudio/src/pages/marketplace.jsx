import { Container, Row, Col, Button, Form, Card, Dropdown, Toast, ToastContainer } from "react-bootstrap";
import { useState, useEffect, useContext } from "react";
import modelImage from "../assets/homepage_model.png";
import "../styles/dashboard.css";
import "../styles/submodelTemplateSelection.css";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";
import PlusIcon from "../assets/icons/plus-lg.svg?react";
import { KeycloakContext } from "../KeycloakContext";
import { authenticatedFetch } from "../utils/tokenManager";

export default function Marketplace() {
    const { keycloak, authenticated } = useContext(KeycloakContext);
    const [entries, setEntries] = useState([]);
    const [tags, setTags] = useState([]);
    const [searchText, setSearchText] = useState("");
    const [selectedTags, setSelectedTags] = useState([]);
    const [loading, setLoading] = useState(true);
    const [downloadFormats, setDownloadFormats] = useState({}); // { entryId: "AASX" | "JSON" }
    const [toasts, setToasts] = useState([]);

    const showToast = (title, message, isError = false) => {
        const toast = {
            id: Date.now(),
            title,
            message,
            isError
        };
        setToasts(prev => [...prev, toast]);
        setTimeout(() => {
            setToasts(prev => prev.filter(t => t.id !== toast.id));
        }, 5000);
    };

    // Handle multi-select tag changes
    const handleTagSelection = (tagId) => {
        setSelectedTags(prev => {
            const newSelectedTags = prev.includes(tagId) 
                ? prev.filter(id => id !== tagId)  // Remove tag if already selected
                : [...prev, tagId];                // Add tag if not selected
            
            // Trigger search automatically after state update
            setTimeout(() => {
                performSearch(searchText, newSelectedTags);
            }, 0);
            
            return newSelectedTags;
        });
    };

    // Clear all selected tags
    const clearAllTags = () => {
        setSelectedTags([]);
        // Trigger search automatically
        setTimeout(() => {
            performSearch(searchText, []);
        }, 0);
    };

    // Extracted search logic for reuse
    const performSearch = async (searchText, tagIds) => {
        // Check if Keycloak is ready
        if (!keycloak || !authenticated) {
            showToast("Authentication required", "Please log in to search the marketplace.", true);
            return;
        }

        setLoading(true);
        try {
            console.log('Performing search with:', { searchText, tagIds });
            const response = await authenticatedFetch("http://localhost:9090/marketplace/search", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    searchText: searchText || undefined,
                    tagIds: tagIds.length > 0 ? tagIds : undefined
                })
            }, keycloak);
            
            if (!response.ok) {
                throw new Error('Search failed');
            }
            
            const data = await response.json();
            setEntries(data);
        } catch (error) {
            console.error('Search failed:', error);
            showToast("Search failed", "Failed to search marketplace. Please try again.", true);
        } finally {
            setLoading(false);
        }
    };

    // 1. Alle Marketplace-Einträge & Tags laden
    useEffect(() => {
        const loadMarketplaceData = async () => {
            // Wait for Keycloak to be initialized
            if (!keycloak || !authenticated) {
                setLoading(false);
                return;
            }

            try {
                // Load marketplace entries
                const entriesResponse = await authenticatedFetch("http://localhost:9090/marketplace", {
                    method: "GET"
                }, keycloak);
                
                if (entriesResponse.ok) {
                    const entriesData = await entriesResponse.json();
                    setEntries(entriesData);
                }

                // Load tags
                const tagsResponse = await authenticatedFetch("http://localhost:9090/marketplace/tags", {
                    method: "GET"
                }, keycloak);
                
                if (tagsResponse.ok) {
                    const tagsData = await tagsResponse.json();
                    setTags(tagsData);
                }
            } catch (error) {
                console.error('Failed to load marketplace data:', error);
                showToast("Loading failed", "Failed to load marketplace data. Please try again.", true);
            } finally {
                setLoading(false);
            }
        };

        loadMarketplaceData();
    }, [keycloak, authenticated]);

    // 2. Suchfunktion – sendet `MarketplaceSearchRequest` an Backend
    const handleSearch = async () => {
        await performSearch(searchText, selectedTags);
    };

    // 3. Download-Funktion mit Formatwahl
    const handleDownload = async (entryId, title) => {
        const format = downloadFormats[entryId] || "AASX";
        try {
            const response = await authenticatedFetch(`http://localhost:9090/marketplace/${entryId}/download?format=${format}`, {
                method: 'GET',
            }, keycloak);

            if (!response.ok) {
                throw new Error('Download failed');
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = `${title}.${format.toLowerCase()}`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            showToast("Download successful", `${title} downloaded successfully!`);
        } catch (error) {
            console.error('Error downloading model:', error);
            showToast("Download failed", "Failed to download the model. Please try again.", true);
        }
    };

    const handleSave = async (entryId, title) => {
        if (!authenticated) {
            showToast("Authentication required", "Please log in to save models.", true);
            return;
        }

        try {
            const response = await authenticatedFetch(`http://localhost:9090/marketplace/${entryId}/add-to-user`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
            }, keycloak);

            if (!response.ok) {
                throw new Error('Save failed');
            }

            showToast("Model saved", `${title} has been saved to your models!`);
        } catch (error) {
            console.error('Error saving model:', error);
            showToast("Save failed", "Failed to save the model. Please try again.", true);
        }
    };    return (
        <div className="dashboard-container">
            <style>{`
                .marketplace-dropdown-toggle:hover {
                    background-color: rgba(255, 255, 255, 0.15) !important;
                    border-color: #0D598B !important;
                }
                .marketplace-dropdown-toggle:focus {
                    background-color: rgba(255, 255, 255, 0.15) !important;
                    border-color: #0D598B !important;
                    box-shadow: 0 0 0 0.2rem rgba(13, 89, 139, 0.25) !important;
                }
            `}</style>
            <Container className="py-4">
                <h2 className="text-white mb-3">Browse and download models for Digital Twins</h2>

                {/* Filterzeile: Suche & Kategorieauswahl */}
                <Row className="mb-3">
                    <Col md={8}>
                        <Form.Control
                            type="text"
                            placeholder="Search by title, description or author"
                            value={searchText}
                            onChange={e => setSearchText(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && handleSearch()}
                            className="search-input"
                        />
                    </Col>
                    <Col md={4}>
                        <Dropdown>
                            <Dropdown.Toggle 
                                variant="outline-light" 
                                className="w-100 d-flex justify-content-between align-items-center marketplace-dropdown-toggle"
                                style={{ 
                                    backgroundColor: 'rgba(255, 255, 255, 0.1)',
                                    border: '2px solid #0E4175',
                                    color: 'white',
                                    borderRadius: '8px'
                                }}
                            >
                                <span>
                                    {selectedTags.length === 0 
                                        ? "All Categories" 
                                        : `${selectedTags.length} category(ies) selected`
                                    }
                                </span>
                            </Dropdown.Toggle>
                            <Dropdown.Menu 
                                className="w-100" 
                                style={{ 
                                    maxHeight: '300px', 
                                    overflowY: 'auto',
                                    backgroundColor: '#00376A',
                                    border: '2px solid #0E4175',
                                    borderRadius: '8px'
                                }}
                            >
                                <Dropdown.Item 
                                    onClick={clearAllTags} 
                                    className="text-primary fw-bold"
                                    style={{ 
                                        backgroundColor: 'transparent',
                                        color: '#4299e1 !important'
                                    }}
                                >
                                    Clear All
                                </Dropdown.Item>
                                <Dropdown.Divider style={{ borderColor: '#0E4175' }} />
                                {[...tags]
                                    .sort((a, b) => a.name.localeCompare(b.name))
                                    .map(tag => (
                                        <Dropdown.Item 
                                            key={tag.id} 
                                            onClick={() => handleTagSelection(tag.id)}
                                            className="d-flex align-items-center text-white"
                                            style={{ 
                                                backgroundColor: 'transparent',
                                                transition: 'background-color 0.2s',
                                                color: 'white'
                                            }}
                                            onMouseEnter={e => e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.1)'}
                                            onMouseLeave={e => e.target.style.backgroundColor = 'transparent'}
                                        >
                                            <Form.Check
                                                type="checkbox"
                                                checked={selectedTags.includes(tag.id)}
                                                onChange={() => {}} // Handled by onClick
                                                className="me-2"
                                            />
                                            {tag.name} ({tag.usageCount})
                                        </Dropdown.Item>
                                    ))}
                            </Dropdown.Menu>
                        </Dropdown>
                    </Col>
                </Row>

                {/* Selected tags display */}
                {selectedTags.length > 0 && (
                    <Row className="mb-3">
                        <Col>
                            <div className="d-flex flex-wrap gap-2">
                                <small className="text-white me-2 align-self-center">Selected categories:</small>
                                {selectedTags.map(tagId => {
                                    const tag = tags.find(t => t.id === tagId);
                                    return tag ? (
                                        <span 
                                            key={tagId} 
                                            className="badge d-flex align-items-center"
                                            style={{
                                                backgroundColor: '#4299e1',
                                                color: 'white',
                                                fontSize: '0.8rem',
                                                padding: '0.4rem 0.6rem'
                                            }}
                                        >
                                            {tag.name}
                                            <button
                                                type="button"
                                                className="btn-close btn-close-white ms-2"
                                                style={{ 
                                                    fontSize: '0.5rem',
                                                    opacity: '0.8'
                                                }}
                                                onClick={() => handleTagSelection(tagId)}
                                                aria-label="Remove tag"
                                            ></button>
                                        </span>
                                    ) : null;
                                })}
                            </div>
                        </Col>
                    </Row>
                )}

                {/* Ladeanzeige / Keine Ergebnisse */}
                {loading && <div className="text-white">Loading...</div>}
                {!loading && entries.length === 0 && <div className="text-white">No entries found.</div>}
                {/* Ergebnisliste */}
                <Row className="g-3">
                    {entries.map(entry => (
                        <Col key={entry.id} md={4}>
                            <Card className="text-white model-container h-100">
                                <Card.Body className="d-flex flex-column">
                                    <div className="d-flex align-items-center mb-2">
                                        <img
                                            src={modelImage}
                                            alt="Model"
                                            style={{ height: 80, marginRight: "0.5rem" }}
                                        />
                                        <div className="flex-grow-1">
                                            <h5 className="mb-1"></h5>
                                            <p className="mb-1" style={{ fontSize: "0.9rem" }}>
                                                {entry.shortDescription || "No description available"}
                                            </p>
                                            <small>Author: {entry.author}</small><br />
                                            <small>Published: {entry.publishedAt?.substring(0, 10)}</small>
                                        </div>
                                    </div>

                                    <div className="mt-auto d-flex flex-column gap-2">
                                        <div className="d-flex gap-2">
                                            <Dropdown>
                                                <Dropdown.Toggle 
                                                    variant="primary" 
                                                    size="sm" 
                                                    className="d-flex align-items-center"
                                                >
                                                    <DownloadIcon className="me-1" style={{ width: '16px', height: '16px' }} /> Download
                                                </Dropdown.Toggle>
                                                <Dropdown.Menu>
                                                    <Dropdown.Item 
                                                        onClick={() => {
                                                            setDownloadFormats(prev => ({...prev, [entry.id]: "AASX"}));
                                                            handleDownload(entry.id, entry.name);
                                                        }}
                                                    >
                                                        Download as AASX
                                                    </Dropdown.Item>
                                                    <Dropdown.Item 
                                                        onClick={() => {
                                                            setDownloadFormats(prev => ({...prev, [entry.id]: "JSON"}));
                                                            handleDownload(entry.id, entry.name);
                                                        }}
                                                    >
                                                        Download as JSON
                                                    </Dropdown.Item>
                                                </Dropdown.Menu>
                                            </Dropdown>
                                            
                                            <Button
                                                size="sm"
                                                variant="outline-success"
                                                onClick={() => handleSave(entry.id, entry.name)}
                                                className="d-flex align-items-center"
                                            >
                                                <PlusIcon className="me-1" style={{ width: '16px', height: '16px' }} /> Save
                                            </Button>
                                        </div>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                    ))}
                </Row>

            </Container>
            
            <ToastContainer position="top-end" className="position-fixed" style={{ top: '20px', right: '20px', zIndex: 1050 }}>
                {toasts.map(toast => (
                    <Toast key={toast.id} bg={toast.isError ? 'danger' : 'success'} text="white">
                        <Toast.Header>
                            <strong className="me-auto">{toast.title}</strong>
                        </Toast.Header>
                        <Toast.Body>{toast.message}</Toast.Body>
                    </Toast>
                ))}
            </ToastContainer>
        </div>
    );
}
