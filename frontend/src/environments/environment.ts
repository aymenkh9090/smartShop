// Valeurs injectees au build par `ng build --define` (voir le script build:netlify).
// Absentes : typeof renvoie 'undefined' (pas d'erreur), on garde le comportement par defaut.
declare const SMARTSHOP_API_URL: string | undefined;
declare const SMARTSHOP_DEMO_USERNAME: string | undefined;
declare const SMARTSHOP_DEMO_PASSWORD: string | undefined;

const apiUrl = typeof SMARTSHOP_API_URL === 'string' && SMARTSHOP_API_URL ? SMARTSHOP_API_URL : '/api';
const demoUsername = typeof SMARTSHOP_DEMO_USERNAME === 'string' ? SMARTSHOP_DEMO_USERNAME : '';
const demoPassword = typeof SMARTSHOP_DEMO_PASSWORD === 'string' ? SMARTSHOP_DEMO_PASSWORD : '';

// Production : par defaut le frontend est servi derriere le meme domaine que l'API (reverse proxy) ;
// sur Netlify, l'API est appelee directement sur son domaine (CORS).
export const environment = {
  production: true,
  apiUrl,
  /** Compte de demonstration affiche sur la page de connexion (portfolio), null sinon. */
  demoAccount: demoUsername && demoPassword ? { username: demoUsername, password: demoPassword } : null,
};
