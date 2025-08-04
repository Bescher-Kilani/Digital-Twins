import { Container, Row, Col, Button, Form, Card, Dropdown, Toast, ToastContainer } from "react-bootstrap";
import { useState, useEffect, useContext } from "react";
import modelImage from "../assets/homepage_model.png";
import "../styles/dashboard.css";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";
import PlusIcon from "../assets/icons/plus-lg.svg?react";
import { KeycloakContext } from "../KeycloakContext";
import { authenticatedFetch } from "../utils/tokenManager";

export default function Marketplace() {
    const { keycloak, authenticated } = useContext(KeycloakContext);
    const [entries, setEntries] = useState([]);
    const [tags, setTags] = useState([]);
    const [searchText, setSearchText] = useState("");
    const [selectedTag, setSelectedTag] = useState("");
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

    // 1. Alle Marketplace-Einträge & Tags laden
    useEffect(() => {
        fetch("http://localhost:9090/marketplace")
            .then(res => res.json())
            .then(setEntries)
            .finally(() => setLoading(false));

        fetch("http://localhost:9090/marketplace/tags")
            .then(res => res.json())
            .then(setTags);
    }, []);

    // 2. Suchfunktion – sendet `MarketplaceSearchRequest` an Backend
    const handleSearch = async () => {
        setLoading(true);
        const res = await fetch("http://localhost:9090/marketplace/search", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                searchText: searchText || undefined,
                tagIds: selectedTag ? [selectedTag] : undefined
            })
        });
        const data = await res.json();
        setEntries(data);
        setLoading(false);
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
                        />
                    </Col>
                    <Col md={4}>
                        <Form.Select
                            value={selectedTag}
                            onChange={e => setSelectedTag(e.target.value)}
                        >
                            <option value="">All Categories</option>
                            {[...tags]
                                .sort((a, b) => a.name.localeCompare(b.name))
                                .map(tag => (
                                    <option key={tag.id} value={tag.id}>
                                        {tag.name} ({tag.usageCount})
                                    </option>
                                ))}
                        </Form.Select>
                    </Col>
                </Row>

                {/* Such-Button */}
                <Row className="mb-3">
                    <Col>
                        <Button variant="primary" onClick={handleSearch}>Search</Button>
                    </Col>
                </Row>

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
