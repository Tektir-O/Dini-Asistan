import fs from "node:fs";

const id="turkish_rwwad";
const surahs=[];
for(let s=1;s<=114;s++){
  const url="https://quranenc.com/api/v1/translation/sura/"+id+"/"+s;
  let response;
  for(let attempt=1;attempt<=4;attempt++){
    try {
      const request=await fetch(url,{signal:AbortSignal.timeout(30000)});
      if(!request.ok)throw new Error("HTTP "+request.status);
      response=await request.json();
      break;
    }catch(error){
      if(attempt===4)throw new Error("Sure "+s+" okunamadı: "+error.message);
      await new Promise(resolve=>setTimeout(resolve,attempt*1500));
    }
  }
  const list=Array.isArray(response?.result)?response.result:
    Array.isArray(response?.data)?response.data:
    Array.isArray(response)?response:null;
  if(!list)throw new Error("Beklenmedik JSON yapısı: "+s);
  surahs.push(list.map((entry,i)=>{
    if(Number(entry.sura)!==s || Number(entry.aya)!==i+1 ||
      typeof entry.translation!=="string" || !entry.translation.trim()){
      throw new Error("Meal ayet hatası: "+s+":"+(i+1));
    }
    return {aya:i+1,translation:entry.translation,footnotes:entry.footnotes??null};
  }));
}
const index=JSON.parse(fs.readFileSync("app/src/main/assets/quran-index.json","utf8"));
let count=0;
for(let s=0;s<114;s++){
  if(surahs[s].length!==index.ayah_pages[s].length){
    throw new Error("Meal ile Kur'an ayet sayısı eşleşmiyor: "+(s+1));
  }
  count+=surahs[s].length;
}
if(count!==6236)throw new Error("6236 ayet bekleniyordu, gelen: "+count);
fs.writeFileSync("app/src/main/assets/meal-rowad.json",JSON.stringify({
    edition:id,version:"1.0.4",source:"https://quranenc.com/tr/browse/turkish_rwwad",
    publisher:"Rowad Tercüme Merkezi",surahs
}));
console.log("Rowad meal oluşturuldu: 114 sure, "+count+" ayet.");
