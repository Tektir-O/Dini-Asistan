import fs from "node:fs";
import path from "node:path";
import { ayahsInPart, findPage, getAyahCountInSurah, getSurahMeta } from "quran-meta/hafs";

const names = [
"Fâtiha","Bakara","Âl-i İmrân","Nisâ","Mâide","En'âm","A'râf","Enfâl","Tevbe","Yûnus",
"Hûd","Yûsuf","Ra'd","İbrâhîm","Hicr","Nahl","İsrâ","Kehf","Meryem","Tâhâ",
"Enbiyâ","Hac","Mü'minûn","Nûr","Furkân","Şuarâ","Neml","Kasas","Ankebût","Rûm",
"Lokmân","Secde","Ahzâb","Sebe'","Fâtır","Yâsîn","Sâffât","Sâd","Zümer","Mü'min",
"Fussilet","Şûrâ","Zuhruf","Duhân","Câsiye","Ahkâf","Muhammed","Fetih","Hucurât","Kâf",
"Zâriyât","Tûr","Necm","Kamer","Rahmân","Vâkıa","Hadîd","Mücâdele","Haşr","Mümtehine",
"Saf","Cum'a","Münâfikûn","Tegâbün","Talâk","Tahrîm","Mülk","Kalem","Hâkka","Meâric",
"Nûh","Cin","Müzzemmil","Müddessir","Kıyâmet","İnsân","Mürselât","Nebe'","Nâziât","Abese",
"Tekvîr","İnfitâr","Mutaffifîn","İnşikâk","Bürûc","Târık","A'lâ","Gâşiye","Fecr","Beled",
"Şems","Leyl","Duhâ","İnşirâh","Tîn","Alak","Kadir","Beyyine","Zilzâl","Âdiyât",
"Kâria","Tekâsür","Asr","Hümeze","Fîl","Kureyş","Mâûn","Kevser","Kâfirûn","Nasr",
"Tebbet","İhlâs","Felak","Nâs"
];
if (names.length !== 114) throw new Error("114 sure adı olmalı.");

const surahs=[], ayahPages=[];
let total=0;
for (let s=1;s<=114;s++) {
    const count=getAyahCountInSurah(s);
    const pages=[];
    for(let a=1;a<=count;a++) {
        const p=findPage(s,a);
        if (!Number.isInteger(p) || p<1 || p>604) throw new Error("Ayet-sayfa hatası: "+s+":"+a);
        pages.push(p);
    }
    surahs.push({ number:s, name:names[s-1], arabic:getSurahMeta(s).name,
        ayahs:count, page:pages[0] });
    ayahPages.push(pages);
    total+=count;
}
if (total!==6236) throw new Error("6236 ayet bekleniyor: "+total);
if (findPage(2,255)!==42) throw new Error("Bakara 2:255 sayfa testi başarısız.");

const juzPages=[];
for(let j=1;j<=30;j++) {
    const first=[...ayahsInPart("juz",j)][0];
    if (!first || first.length!==2) throw new Error("Cüz indeksi eksik: "+j);
    juzPages.push(findPage(first[0],first[1]));
}
const dir="app/src/main/assets";
fs.mkdirSync(dir,{recursive:true});
fs.writeFileSync(path.join(dir,"quran-index.json"),
    JSON.stringify({edition:"hafs-medina-604",surahs,ayah_pages:ayahPages,juz_pages:juzPages}));
console.log("Tamam: 114 sure, "+total+" ayet, 30 cüz, 604 sayfa eşlemesi.");
