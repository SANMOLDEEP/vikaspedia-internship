window.global = window; // 👈 ADD THIS LINE

import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  // ❌ remove StrictMode for now
  <App />
);