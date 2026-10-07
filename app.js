const API = "https://api.alquran.cloud/v1";

const els = {
  libraryView: document.querySelector("#libraryView"),
  readerView: document.querySelector("#readerView"),
  surahList: document.querySelector("#surahList"),
  status: document.querySelector("#status"),
  readerStatus: document.querySelector("#readerStatus"),
  searchInput: document.querySelector("#searchInput"),
  backButton: document.querySelector("#backButton"),
  homeButton: document.querySelector("#homeButton"),
  surahTitle: document.querySelector("#surahTitle"),
  surahArabicTitle: document.querySelector("#surahArabicTitle"),
  surahMeta: document.querySelector("#surahMeta"),
  ayahList: document.querySelector("#ayahList"),
};

let surahs = [];
let turkishEdition = null;

function normalize(value = "") {
  return value
    .toLocaleLowerCase("tr-TR")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9çğıöşü\s-]/gi, "");
}

function setView(name) {
  const reading = name === "reader";
  els.libraryView.hidden = reading;
  els.readerView.hidden = !reading;
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderSurahs(items) {
  els.surahList.innerHTML = "";

  if (!items.length) {
    els.status.textContent = "Aramana uygun sure bulunamadı.";
    return;
  }

  els.status.textContent = `${items.length} sure gösteriliyor.`;

  for (const surah of items) {
    const button = document.createElement("button");
    button.className = "surah-card";
    button.type = "button";
    button.innerHTML = `
      <span class="surah-number">${surah.number}</span>
      <span class="surah-name">
        <strong>${surah.englishName}</strong>
        <small>${surah.numberOfAyahs} ayet · ${surah.revelationType === "Meccan" ? "Mekki" : "Medeni"}</small>
      </span>
      <span class="surah-arabic" lang="ar" dir="rtl">${surah.name}</span>
    `;
    button.addEventListener("click", () => openSurah(surah.number));
    els.surahList.appendChild(button);
  }
}

async function getTurkishEdition() {
  try {
    const response = await fetch(`${API}/edition/language/tr`);
    if (!response.ok) throw new Error("Türkçe meal listesi alınamadı.");
    const payload = await response.json();
    const editions = Array.isArray(payload.data) ? payload.data : [];

    return (
      editions.find((item) =>
        item.type === "translation" &&
        /diyanet/i.test(`${item.identifier} ${item.name} ${item.englishName}`)
      ) ||
      editions.find((item) => item.type === "translation") ||
      null
    );
  } catch {
    return null;
  }
}

async function loadSurahs() {
  try {
    els.status.textContent = "Sureler yükleniyor…";
    const [surahResponse, edition] = await Promise.all([
      fetch(`${API}/surah`),
      getTurkishEdition(),
    ]);

    if (!surahResponse.ok) throw new Error("Sure listesi alınamadı.");

    const payload = await surahResponse.json();
    surahs = payload.data || [];
    turkishEdition = edition;

    renderSurahs(surahs);

    const saved = Number(localStorage.getItem("lastSurah"));
    if (saved >= 1 && saved <= 114) {
      els.status.textContent += ` Son okunan sure: ${saved}.`;
    }
  } catch (error) {
    els.status.textContent = "Kur'an verileri şu anda yüklenemedi. İnternet bağlantını kontrol et.";
    console.error(error);
  }
}

function pairAyahs(data) {
  if (!Array.isArray(data)) return { arabic: [], translation: [] };

  const arabicEdition =
    data.find((item) => item.edition?.language === "ar" || item.edition?.type === "quran") ||
    data[0];

  const translationEdition =
    data.find((item) => item.edition?.language === "tr") ||
    data.find((item) => item.edition?.type === "translation");

  return {
    arabic: arabicEdition?.ayahs || [],
    translation: translationEdition?.ayahs || [],
  };
}

async function openSurah(number) {
  const meta = surahs.find((item) => item.number === number);
  if (!meta) return;

  setView("reader");
  els.ayahList.innerHTML = "";
  els.readerStatus.textContent = "Sure yükleniyor…";
  els.surahTitle.textContent = meta.englishName;
  els.surahArabicTitle.textContent = meta.name;
  els.surahMeta.textContent =
    `${meta.number}. sure · ${meta.numberOfAyahs} ayet · ${meta.revelationType === "Meccan" ? "Mekki" : "Medeni"}`;

  localStorage.setItem("lastSurah", String(number));
  history.replaceState(null, "", `#sure-${number}`);

  try {
    const editions = ["quran-uthmani"];
    if (turkishEdition?.identifier) editions.push(turkishEdition.identifier);

    const response = await fetch(
      `${API}/surah/${number}/editions/${encodeURIComponent(editions.join(","))}`
    );

    if (!response.ok) throw new Error("Sure yüklenemedi.");

    const payload = await response.json();
    const { arabic, translation } = pairAyahs(payload.data);

    if (!arabic.length) throw new Error("Ayet bulunamadı.");

    const fragment = document.createDocumentFragment();

    arabic.forEach((ayah, index) => {
      const card = document.createElement("article");
      card.className = "ayah";

      const trText = translation[index]?.text;
      card.innerHTML = `
        <div class="ayah-top">
          <span class="ayah-badge">Ayet ${ayah.numberInSurah}</span>
        </div>
        <p class="arabic-text" lang="ar" dir="rtl">${ayah.text}</p>
        ${trText ? `<p class="translation">${trText}</p>` : ""}
      `;

      fragment.appendChild(card);
    });

    els.ayahList.appendChild(fragment);
    els.readerStatus.textContent = turkishEdition
      ? `Türkçe meal: ${turkishEdition.name || turkishEdition.englishName || turkishEdition.identifier}`
      : "Türkçe meal bulunamadı; Arapça metin gösteriliyor.";
  } catch (error) {
    els.readerStatus.textContent =
      "Bu sure şu anda yüklenemedi. İnternet bağlantını kontrol edip tekrar dene.";
    console.error(error);
  }
}

function goHome() {
  history.replaceState(null, "", location.pathname);
  setView("library");
  els.searchInput.focus();
}

els.searchInput.addEventListener("input", (event) => {
  const query = normalize(event.target.value.trim());

  if (!query) {
    renderSurahs(surahs);
    return;
  }

  const filtered = surahs.filter((surah) => {
    const haystack = normalize(
      `${surah.number} ${surah.englishName} ${surah.englishNameTranslation} ${surah.name}`
    );
    return haystack.includes(query);
  });

  renderSurahs(filtered);
});

els.backButton.addEventListener("click", goHome);
els.homeButton.addEventListener("click", goHome);

window.addEventListener("DOMContentLoaded", async () => {
  await loadSurahs();

  const match = location.hash.match(/^#sure-(\d{1,3})$/);
  if (match) {
    const number = Number(match[1]);
    if (number >= 1 && number <= 114) {
      await openSurah(number);
    }
  }
});

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("./service-worker.js").catch(() => {});
  });
}
