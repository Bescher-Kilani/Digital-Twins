import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import Home from "./pages/home"; 
import NotFound from "./pages/NotFound";
import Marketplace from "./pages/marketplace";
import Signin from "./pages/sign_in";
import CreatePage from "./pages/createPage";
import Dashboard from "./pages/dashboard";
import SubmodelTemplateSelection from "./pages/SubmodelTemplateSelection";
import CreateTemplate from "./pages/createTemplate";
import CreateComplete from "./pages/createComplete";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="signin" element={<Signin />} />
          <Route path="modelhub" element={
            <ProtectedRoute>
              <Marketplace />
            </ProtectedRoute>
          } />
          <Route path="create" element={<CreatePage />} />
          <Route path="create/:modelId" element={<CreatePage />} />
          <Route path="create/complete" element={<CreateComplete />} />
          <Route path="templates" element={<SubmodelTemplateSelection />} />
          <Route path="templates/create" element={<CreateTemplate />} />
          <Route path="dashboard" element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
