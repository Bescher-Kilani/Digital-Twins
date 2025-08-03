import { Container, Row, Col, Button, Form, Card } from "react-bootstrap";
import { useState, useEffect } from "react";
import modelImage from "../assets/homepage_model.png";
import "../styles/dashboard.css";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";

export default function Marketplace() {
    const [entries, setEntries] = useState([]);
    const [tags, setTags] = useState([]);
    const [searchText, setSearchText] = useState("");
    const [selectedTag, setSelectedTag] = useState("");
    const [loading, setLoading] = useState(true);
    const [downloadFormats, setDownloadFormats] = useState({}); // { entryId: "AASX" | "JSON" }

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
    const handleDownload = async (entryId) => {
        const format = downloadFormats[entryId] || "AASX";
        const url = `http://localhost:9090/marketplace/${entryId}?format=${format.toLowerCase()}`;
        const filename = `model-${entryId}`;

        try {
            const res = await fetch(url);
            const blob = await res.blob();
            const link = document.createElement("a");
            link.href = URL.createObjectURL(blob);
            link.download = `${filename}.${format.toLowerCase()}`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(link.href);
        } catch (error) {
            alert("Download failed: " + error.message);
        }
    };

    return (
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
                                        <Form.Select
                                            size="sm"
                                            value={downloadFormats[entry.id] || "AASX"}
                                            onChange={e =>
                                                setDownloadFormats(prev => ({
                                                    ...prev,
                                                    [entry.id]: e.target.value
                                                }))
                                            }
                                        >
                                            <option value="AASX">AASX</option>
                                            <option value="JSON">JSON</option>
                                        </Form.Select>

                                        <Button
                                            size="sm"
                                            variant="primary"
                                            onClick={() => handleDownload(entry.id)}
                                        >
                                            <DownloadIcon /> Download
                                        </Button>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                    ))}
                </Row>

            </Container>
        </div>
    );
}
