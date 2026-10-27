/* Deterministic 16x16 terrain sprite generator for the RPG biome palette.
 * A texture is identified by its hexadecimal palette color, type (0 strata,
 * 1 trunk, 2 foliage, 3 flowering grass, 4 ground) and seed. Uses no libraries and
 * returns PNG binary as base64; safe to reproduce original assets.
 */
function makeBiomePng(rgb,type,seed){
    const pigment=[1,3,5].map(i=>parseInt(rgb.slice(i,i+2),16));
    const raw=[];let rng=seed;
    for(let y=0;y<16;y++){raw.push(0);
        for(let x=0;x<16;x++){
            rng=(Math.imul(rng,1664525)+1013904223)|0;
            const n=(rng>>>12)&255;
            let shade=type===0?(Math.floor(y/4)%2?-.06:.08)+((x*5+y)%9===0?-.18:0)
                :type===1?(x%4===0?-.24:.06)+(y%7===0?-.1:0)
                :type===2?(n<65?-.25:.08)
                :type===4?((x*3+y*5)%11===0?.28:-.12)+(Math.floor(y/3)%2?.045:-.035):.15;
            let alpha=255;
            if(type===2 && n<44) alpha=0;
            if(type===3){
                const stem=x===7&&y>=7;
                const petal=Math.abs(x-7)+Math.abs(y-5)<5&&((x+y)%3!==0);
                const core=Math.abs(x-7)+Math.abs(y-5)<2;
                alpha=stem||petal||core?255:0;
                shade=stem?-.38:core?.35:shade;
            }
            raw.push(...pigment.map(c=>Math.max(0,Math.min(255,Math.round(c*(.76+n/950+shade))))),alpha);
        }
    }
    const u32=n=>[(n>>>24)&255,(n>>>16)&255,(n>>>8)&255,n&255];
    let adlerA=1,adlerB=0;
    for(const v of raw){adlerA=(adlerA+v)%65521;adlerB=(adlerB+adlerA)%65521}
    function crc(data){let c=-1;for(const v of data){c^=v;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0)}return(c^-1)>>>0}
    function chunk(name,data){
        const tag=[...name].map(c=>c.charCodeAt(0));
        return [...u32(data.length),...tag,...data,...u32(crc([...tag,...data]))];
    }
    const zlib=[120,1,1,raw.length&255,raw.length>>>8,(~raw.length)&255,((~raw.length)>>>8)&255,...raw,...u32((adlerB<<16)|adlerA)];
    const png=[137,80,78,71,13,10,26,10,...chunk('IHDR',[...u32(16),...u32(16),8,6,0,0,0]),...chunk('IDAT',zlib),...chunk('IEND',[])];
    const alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    let base64='';
    for(let i=0;i<png.length;i+=3){
        const a=png[i],b=png[i+1]||0,c=png[i+2]||0;
        base64+=alphabet[a>>>2]+alphabet[((a&3)<<4)|(b>>>4)]
            +(i+1<png.length?alphabet[((b&15)<<2)|(c>>>6)]:'=')
            +(i+2<png.length?alphabet[c&63]:'=');
    }
    return base64;
}
