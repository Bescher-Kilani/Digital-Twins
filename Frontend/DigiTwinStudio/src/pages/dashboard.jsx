import { Container, Row, Col, Button, Form, Card } from "react-bootstrap";
import modelImage from "../assets/homepage_model.png";
import "../styles/dashboard.css";

const models = [
  {
    title: "Building 1",
    description: "Digital Twin of a building",
    lastEdit: "26. June 2025",
  },
  {
    title: "Example Model",
    description: "An example model",
    lastEdit: "26. June 2025",
  },
  {
    title: "Another Example Model",
    description: "Another example model",
    lastEdit: "26. June 2025",
  },
];

export default function Dashboard() {
  return (
    <div className="dashboard-container">
        <Container className="py-4">
      <h2 className="text-white mb-3">Your Models</h2>

      {/* Actions */}
      <Row className="mb-3">
        <Col md={8}>
          <Form.Control
            type="text"
            placeholder="Search"
            className="mb-2"
          />
        </Col>
        <Col md={4} className="text-md-end">
          <Button variant="primary" className="me-2">
            ↑ Import Model
          </Button>
          <Button variant="primary">+ New Model</Button>
        </Col>
      </Row>

      {/* Models list */}
      <div className="d-flex flex-column gap-3">
        {models.map((model, index) => (
          <Card key={index} className="text-white model-container">
            <Card.Body className="d-flex align-items-center">
              <img
                src={modelImage}
                alt="Model"
                style={{ height: 100, marginRight: "1rem" }}
              />
              <div className="flex-grow-1">
                <h5 className="mb-1">{model.title}</h5>
                <p className="mb-1">{model.description}</p>
                <small>Last edit: {model.lastEdit}</small>
              </div>
              <div className="d-flex flex-column gap-2">
                <Button size="sm" variant="primary">
                  ↗ Open
                </Button>
                <Button size="sm" variant="primary">
                  ↓ Download
                </Button>
              </div>
            </Card.Body>
          </Card>
        ))}
      </div>
    </Container>
    </div>
  );
}
