import '../styles/Header.css'; // Importer le CSS pour le Header

const Header = () => {
    return (
        <header className="header">
            <div className="logo">Mon Dashboard</div>
            <div className="nav-links">
                <a href="/buy-crypto">Buy Crypto</a>
                <a href="/buy-action">Buy Action</a>
                <a href="/buy-devise">Buy Devise</a>
                <a href="/wallet">Wallet</a>
            </div>
            <button className="nav-button">Login</button>
        </header>
    );
};

export default Header;