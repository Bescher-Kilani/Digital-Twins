import { useTranslation } from "react-i18next";

function Home() {

  const { t } = useTranslation();

  return (
    <div className="text-center">
      <h1>{t("welcome")}</h1>
      <p>This is the index page built with React.</p>
    </div>
  );
}

export default Home;