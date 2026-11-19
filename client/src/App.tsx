import { useState } from "react";
import { MainLayout } from "./layouts/MainLayout";
import { RegistrationPage } from "./pages/RegistrationPage";
import "./App.css";

function App() {
  const [darkMode, setDarkMode] = useState(false);
  return (
    <MainLayout
      darkMode={darkMode}
      onToggleTheme={() => setDarkMode((mode) => !mode)}
    >
      <RegistrationPage />
    </MainLayout>
  );
}

export default App;
