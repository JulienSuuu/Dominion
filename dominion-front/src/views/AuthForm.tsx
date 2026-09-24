import React, {useState} from 'react';
import { preloadAllCards} from "../PreLoader";
import {AuthFormData} from "../interfaces/Form";
import {User} from "../interfaces/User";
import {Theme} from "../interfaces/Theme";


export interface AuthFormProps {
    user : User;
    setUser : React.Dispatch<React.SetStateAction<User>>;
    connectToServer : (data : AuthResponse) => void;
}

interface LoginPayload {
    email : string;
    password : string;
}
interface RegisterPayload extends LoginPayload {
    pseudo: string;
}

interface AuthResponse {
    id : string;
    pseudo: string;
    email: string;
    theme : Theme
}

interface ApiErrorResponse {
    error?: string;
    message?: string;
}


export default function AuthForm({ user, setUser, connectToServer } : AuthFormProps) {
    const [formData, setFormData] = useState<AuthFormData>({ email: "", pseudo: "" });
    const [password, setPassword] = useState<string>("");

    const handleInputChange = (field: keyof AuthFormData, value: string) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleAuthSubmit = async (e : React.SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();
        const endpoint = user.isRegistering ? '/api/auth/register' : '/api/auth/login';

        if (!formData.email || !password || (user.isRegistering && !formData.pseudo)) {
            alert("Veuillez remplir tous les champs.");
            return;
        }

        const payload: LoginPayload | RegisterPayload = user.isRegistering
            ? { pseudo: formData.pseudo, email: formData.email, password }
            : { email: formData.email, password };

        const baseUrl : string = import.meta.env.VITE_API_URL ?? "http://localhost:3232"


        try {
            const response = await fetch(`${baseUrl}${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                const data : AuthResponse = await response.json();
                if (user.isRegistering) {
                    alert("Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
                    setUser(prev => ({ ...prev, isRegistering: false }));
                } else {
                    preloadAllCards();
                    connectToServer(data);
                    setFormData({ email: '', pseudo: '' });
                    setPassword('');
                }
            } else {
                const errData: ApiErrorResponse = await response.json().catch(() => ({}));
                alert(
                    errData.error ||
                    errData.message ||
                    (user.isRegistering ? "Erreur lors de l'inscription !" : "Identifiants incorrects !")
                );
            }
        } catch (err) {
            console.error("Erreur d'authentification :", err);
            alert("Impossible de joindre le serveur.");
        }
    };

    return (
        <div className="hub-layout">
            <div className="hub-parchment login-box" style={{ maxWidth: '400px', margin: '100px auto', textAlign: 'center' }}>
                <div style={{ display: 'flex', justifyContent: 'space-around', marginBottom: '20px', borderBottom: '1px solid #5c4033' }}>
                    <button
                        type="button"
                        style={{ background: 'none', border: 'none', fontSize: '18px', fontWeight: !user.isRegistering ? 'bold' : 'normal', cursor: 'pointer', paddingBottom: '5px' }}
                        onClick={() => setUser(prev => ({ ...prev, isRegistering: false }))}
                    >
                        Connexion
                    </button>
                    <button
                        type="button"
                        style={{ background: 'none', border: 'none', fontSize: '18px', fontWeight: user.isRegistering ? 'bold' : 'normal', cursor: 'pointer', paddingBottom: '5px' }}
                        onClick={() => setUser(prev => ({ ...prev, isRegistering: true }))}
                    >
                        Créer un compte
                    </button>
                </div>

                <form onSubmit={handleAuthSubmit}>
                    {user.isRegistering && (
                        <input
                            type="text"
                            className="preset-tag"
                            style={{ width: '80%', padding: '10px', fontSize: '16px', marginBottom: '10px', textAlign: 'center' }}
                            placeholder="Pseudo..."
                            value={formData.pseudo}
                            onChange={(e) => handleInputChange('pseudo', e.target.value)}
                            required
                        />
                    )}
                    <input
                        type="email"
                        className="preset-tag"
                        style={{ width: '80%', padding: '10px', fontSize: '16px', marginBottom: '10px', textAlign: 'center' }}
                        placeholder="Email..."
                        value={formData.email}
                        onChange={(e) => handleInputChange('email', e.target.value)}
                        required
                    />
                    <input
                        type="password"
                        className="preset-tag"
                        style={{ width: '80%', padding: '10px', fontSize: '16px', marginBottom: '20px', textAlign: 'center' }}
                        placeholder="Mot de passe..."
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <br />
                    <button type="submit" className="start-button">
                        {user.isRegistering ? "S'INSCRIRE" : "SE CONNECTER"}
                    </button>
                </form>
            </div>
        </div>
    );
}