import { useState } from 'react';

export default function SourceChip({ source }) {
  const [expanded, setExpanded] = useState(false);
  return (
    <div>
      <div onClick={() => setExpanded(!expanded)}
        style={{ fontSize:11, padding:'3px 10px', borderRadius:20,
          cursor:'pointer', background:'#e8f5e9', color:'#2e7d32',
          border:'1px solid #a5d6a7', userSelect:'none' }}>
        📄 {source.filename} · chunk {source.chunkIndex}
      </div>
      {expanded && (
        <div style={{ marginTop:4, padding:'8px 12px', background:'#f9fbe7',
          border:'1px solid #e6ee9c', borderRadius:8, fontSize:12,
          color:'#555', maxWidth:400, lineHeight:1.5 }}>
          {source.preview}
        </div>
      )}
    </div>
  );
}