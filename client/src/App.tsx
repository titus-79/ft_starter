import { useState } from "react";
import { MainLayout } from "./layouts/MainLayout";
import { RegistrationPage } from "./pages/RegistrationPage";
import "./App.css";

type ThemeMode = "light" | "dark" | "rainbow";

function App() {
  const [theme, setTheme] = useState<ThemeMode>("light");

  function cycleTheme() {
    setTheme((current) =>
      current === "light" ? "dark" : current === "dark" ? "rainbow" : "light",
    );
  }

  return (
    <MainLayout theme={theme} onCycleTheme={cycleTheme}>
      <RegistrationPage />
    </MainLayout>
  );
}

export default App;
