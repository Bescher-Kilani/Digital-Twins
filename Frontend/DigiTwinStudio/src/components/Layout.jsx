import CustomNavbar from "./CustomNavbar";
import Footer from "./Footer";
import { Outlet } from "react-router-dom";

function Layout() {
  return (
    <div className="d-flex flex-column min-vh-100">
      <CustomNavbar />
      
      <main className="flex-fill">
        <Outlet />
      </main>
      
      <Footer />
    </div>
  );
}

export default Layout;
