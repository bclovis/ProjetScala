//frontend/src/components/Header.jsx
import React from "react";
import { Link } from "react-router-dom";

const Header = () => {
    return (
        <header className="flex justify-between items-center p-4 bg-gray-800 text-white">
            <div className="logo text-xl font-bold">
                Mon Dashboard
            </div>
            <nav className="flex space-x-4">
                <Link to="/buy-crypto" className="hover:underline">Buy Crypto</Link>
                <Link to="/buy-action" className="hover:underline">Buy Action</Link>
                <Link to="/buy-devise" className="hover:underline">Buy Devise</Link>
                <Link to="/wallet" className="hover:underline">Wallet</Link>
            </nav>
        </header>
    );
};

export default Header;