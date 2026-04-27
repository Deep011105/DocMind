import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../services/api';
import { saveToken } from '../services/auth';

export default function RegisterPage() {
  const [form, setForm] = useState({ username:'', email:'', password:'' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await register(form);
      saveToken(res.data.token);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight:'100vh', display:'flex', alignItems:'center',
      justifyContent:'center', background:'#f8f9fa' }}>
      <div style={{ background:'white', borderRadius:12, padding:40, width:380,
        boxShadow:'0 2px 20px rgba(0,0,0,0.08)' }}>
        <div style={{ fontSize:28, fontWeight:700, color:'#0066cc',
          marginBottom:4 }}>DocMind</div>
        <p style={{ color:'#666', marginBottom:28, fontSize:14 }}>
          Create your account
        </p>
        {error && (
          <div style={{ background:'#fff0f0', border:'1px solid #ffcccc',
            borderRadius:8, padding:'10px 14px', color:'#cc0000',
            marginBottom:16, fontSize:13 }}>{error}</div>
        )}
        <form onSubmit={handleSubmit}>
          {['username','email','password'].map(field => (
            <div key={field} style={{ marginBottom:16 }}>
              <label style={{ display:'block', fontSize:13, fontWeight:500,
                color:'#333', marginBottom:6, textTransform:'capitalize' }}>
                {field}
              </label>
              <input
                style={{ width:'100%', padding:'10px 12px',
                  border:'1px solid #ddd', borderRadius:8, fontSize:14,
                  boxSizing:'border-box', outline:'none' }}
                type={field==='password'?'password':field==='email'?'email':'text'}
                value={form[field]}
                onChange={e => setForm({...form,[field]:e.target.value})}
                required
                minLength={field === 'password' ? 8 : field === 'username' ? 3 : undefined}
                title={field === 'password' ? 'Password must be at least 8 characters' : field === 'username' ? 'Username must be at least 3 characters' : ''}
              />
            </div>
          ))}
          <button style={{ width:'100%', padding:12, background:'#0066cc',
            color:'white', border:'none', borderRadius:8, fontSize:15,
            fontWeight:600, cursor:'pointer', marginTop:8 }}
            type="submit" disabled={loading}>
            {loading ? 'Creating account...' : 'Create account'}
          </button>
        </form>
        <p style={{ textAlign:'center', marginTop:20, fontSize:13, color:'#666' }}>
          Have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}