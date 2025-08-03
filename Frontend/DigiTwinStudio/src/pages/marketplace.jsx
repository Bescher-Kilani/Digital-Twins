import { Container, Row, Col, Button, Form, Card, Pagination, InputGroup, Dropdown, Toast, ToastContainer } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useState, useEffect, useContext } from "react";
import modelImage from "../assets/homepage_model.png";
import PlusIcon from "../assets/icons/plus-lg.svg?react";
import searchIcon from "../assets/icons/search.svg";
import { KeycloakContext } from "../KeycloakContext";
import { authenticatedFetch } from "../utils/tokenManager";
import "../styles/marketplace.css";

export default function Marketplace() {
    const { t } = useTranslation();
    const { keycloak, authenticated } = useContext(KeycloakContext);
    const [currentPage, setCurrentPage] = useState(1);
    const [models, setModels] = useState([]);
    const [toasts, setToasts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [searchTerm, setSearchTerm] = useState({
        text: "",
        date: "",
        tags: []
    });
    const navigate = useNavigate();
    const modelsPerPage = 6;

    // Fetch models from API
    useEffect(() => {
        const fetchModels = async () => {
            try {
                setLoading(true);
                setError(null);

                // Prepare headers
                const headers = {
                    'Content-Type': 'application/json'
                };

                // Add Authorization header if user is logged in
                let token = null;

                // Try multiple sources for the token
                token = sessionStorage.getItem('access_token') ||
                    localStorage.getItem('authToken') ||
                    (keycloak && keycloak.token) ||
                    null;

                if (token) {
                    headers['Authorization'] = `Bearer ${token}`;
                }

                const response = await fetch('http://localhost:9090/marketplace', {
                    method: 'GET',
                    headers: headers
                });

                if (!response.ok) {
                    throw new Error(`Failed to fetch models: ${response.status} ${response.statusText}`);
                }

                const data = await response.json();

                // Transform the API data to match the expected format for the UI
                const transformedModels = data.map(model => ({
                    id: model.id,
                    author: model.author?.[0]?.text || 'Unknown',
                    title: model.title?.[0]?.text || 'Untitled Model',
                    //title: model.title,
                    description: model.shortDescription?.[0]?.text || 'No description available',
                    publishedAt: model.publishedAt ? new Date(model.updatedAt).toLocaleDateString('en-US', {
                        day: 'numeric',
                        month: 'long',
                        year: 'numeric'
                    }) : 'Unknown'
                }));

                setModels(transformedModels);
            } catch (err) {
                console.error('Error fetching models:', err);
                setError(err.message);
                // Fall back to empty array on error
                setModels([]);
            } finally {
                setLoading(false);
            }
        };

        fetchModels();
    }, [keycloak, authenticated]);

    // Calculate pagination
    const indexOfLastModel = currentPage * modelsPerPage;
    const indexOfFirstModel = indexOfLastModel - modelsPerPage;
    const currentModels = models.slice(indexOfFirstModel, indexOfLastModel);
    const totalPages = Math.ceil(models.length / modelsPerPage);

    const handlePageChange = (pageNumber) => {
        setCurrentPage(pageNumber);
    };

    const handleNewModel = () => {
        navigate('/create');
    };

    const handleDashboard = () => {
        navigate(authenticated ? "/dashboard" : "/signin");
    }

    const handleSave = async (entry) => {
        if (!entry.id) {
            showToast('Model information is missing. Cannot save file.', 'danger');
            return;
        }

        const url = `http://localhost:9090/marketplace/${entry.id}/add-to-user`;

        try {
            let response;

            if (authenticated) {
                response = await authenticatedFetch(url, {
                    method: 'POST'
                }, keycloak);
            } else {
                showToast("You must be logged in to save a model.", "warning");
                return;
            }

            if (response.ok) {
                showToast("Model saved to your dashboard!", "success");
            } else {
                const errorData = await response.json().catch(() => ({}));
                const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;

                if (response.status === 401) {
                    showToast("Authentication required. Please sign in.", "warning");
                } else if (response.status === 403) {
                    showToast("Access denied. You don't have permission.", "danger");
                } else {
                    showToast(`Failed to save model: ${errorMessage}`, "danger");
                }
            }
        } catch (error) {
            console.error("Save model error:", error);
            showToast("Network error: Could not save model.", "danger");
        }
    }

    // Function to show toast notifications
    const showToast = (message, variant = 'danger') => {
        const id = Date.now();
        const newToast = {
            id,
            message,
            variant,
            show: true
        };
        setToasts(prev => [...prev, newToast]);

        // Auto-hide toast after 5 seconds
        setTimeout(() => {
            setToasts(prev => prev.filter(toast => toast.id !== id));
        }, 5000);
    };

    // Function to manually close a toast
    const closeToast = (id) => {
        setToasts(prev => prev.filter(toast => toast.id !== id));
    };

    return (
        <div className="marketplace-container">
            <Container className="py-4">
                <h2 className="text-white mb-3">{t("marketplace.ourModels")}</h2>

                {/* Search */}
                <Row className="mb-3">
                    <Col md={8}>
                        <InputGroup>
                            <Form.Control
                                className="search-input"
                                type="text"
                                placeholder="Search models..."
                                value={searchTerm.text}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                            <Form.Control
                                className="search-input"
                                type="date"
                                placeholder="published after"
                                value={searchTerm.date}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                            <Form.Control
                                className="search-input"
                            />
                            <Button variant="primary" className="search-icon">
                                <img src={searchIcon} alt={t("marketplace.search")} width="16" height="16" className="search-icon-img" />
                            </Button>
                        </InputGroup>
                    </Col>
                    <Col md={4} className="text-md-end">
                        <Button variant="primary" onClick={handleNewModel}> {t("marketplace.newModel")}</Button>
                    </Col>
                </Row>

                {/* Error message */}
                {error && (
                    <Row className="mb-3">
                        <Col>
                            <div className="alert alert-danger" role="alert">
                                <strong>Error loading models:</strong> {error}
                            </div>
                        </Col>
                    </Row>
                )}

                {/* Loading state */}
                {loading && (
                    <Row className="mb-3">
                        <Col className="text-center">
                            <div className="text-white">Loading models...</div>
                        </Col>
                    </Row>
                )}

                {/* Entries list */}
                <div className="d-flex flex-column gap-3">
                    {!loading && !error && models.length === 0 && (
                        <Card className="text-white model-container">
                            <Card.Body className="text-center py-5">
                                <h5>No models published yet</h5>
                                <p>Let's create and publish the first model!</p>
                                <Button variant="primary" onClick={handleDashboard}>
                                    To the dashboard
                                </Button>
                            </Card.Body>
                        </Card>
                    )}

                    {!loading && currentModels.map((entry, index) => (
                        <Card key={index} className="text-white model-container">
                            <Card.Body className="d-flex align-items-center">
                                <img
                                    src={modelImage}
                                    alt="Model"
                                    style={{ height: 100, marginRight: "1rem" }}
                                />
                                <div className="flex-grow-1">
                                    <h5 className="mb-1">{t("marketplace.title")}{entry.title}</h5>
                                    <p className="mb-1">{entry.description}</p>
                                    <small>{t("marketplace.publishedBy")} {entry.author}</small>
                                    <br />
                                    <small>{t("marketplace.publishedAt")} {entry.publishedAt}</small>
                                </div>
                                <div className="d-flex flex-column gap-2">
                                    <Button
                                        size="sm"
                                        variant="primary"
                                        onClick={() => handleSave(entry)}
                                    >
                                        <PlusIcon></PlusIcon> {t("marketplace.save")}
                                    </Button>
                                </div>
                            </Card.Body>
                        </Card>
                    ))}
                </div>

                {/* Pagination */}
                {!loading && totalPages > 1 && models.length > 0 && (
                    <Row className="mt-4">
                        <Col className="d-flex justify-content-center">
                            <Pagination>
                                <Pagination.First
                                    onClick={() => handlePageChange(1)}
                                    disabled={currentPage === 1}
                                />
                                <Pagination.Prev
                                    onClick={() => handlePageChange(currentPage - 1)}
                                    disabled={currentPage === 1}
                                />

                                {[...Array(totalPages)].map((_, index) => {
                                    const pageNumber = index + 1;
                                    return (
                                        <Pagination.Item
                                            key={pageNumber}
                                            active={pageNumber === currentPage}
                                            onClick={() => handlePageChange(pageNumber)}
                                        >
                                            {pageNumber}
                                        </Pagination.Item>
                                    );
                                })}

                                <Pagination.Next
                                    onClick={() => handlePageChange(currentPage + 1)}
                                    disabled={currentPage === totalPages}
                                />
                                <Pagination.Last
                                    onClick={() => handlePageChange(totalPages)}
                                    disabled={currentPage === totalPages}
                                />
                            </Pagination>
                        </Col>
                    </Row>
                )}
            </Container>

            {/* Toast notifications positioned at top right */}
            <ToastContainer
                position="top-end"
                className="p-3"
                style={{
                    position: 'fixed',
                    top: '20px',
                    right: '20px',
                    zIndex: 9999
                }}
            >
                {toasts.map((toast) => (
                    <Toast
                        key={toast.id}
                        show={toast.show}
                        onClose={() => closeToast(toast.id)}
                        bg={toast.variant}
                        text={toast.variant === 'warning' ? 'dark' : 'white'}
                        autohide
                        delay={5000}
                    >
                        <Toast.Header>
                            <strong className="me-auto">
                                {toast.variant === 'danger' ? 'Error' :
                                    toast.variant === 'warning' ? 'Warning' :
                                        toast.variant === 'success' ? 'Success' : 'Notification'}
                            </strong>
                        </Toast.Header>
                        <Toast.Body>
                            {toast.message}
                        </Toast.Body>
                    </Toast>
                ))}
            </ToastContainer>
        </div>
    );
}