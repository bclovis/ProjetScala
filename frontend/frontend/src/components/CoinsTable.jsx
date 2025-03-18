import React, { useState, useEffect, useRef } from "react";
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, TextField, MenuItem, Select, Dialog } from "@mui/material";
import CoinInfo from "./CoinInfo";
import {grey} from "@mui/material/colors";

const CoinsTable = ({ portfolioId, token, walletBalance, availableBalance, onAssetAdded }) => {
  const [category, setCategory] = useState("crypto"); // Catégorie sélectionnée
  const [searchTerm, setSearchTerm] = useState(""); // Recherche utilisateur
  const [assets, setAssets] = useState([]); // Stockage des actifs (crypto, actions, devises)
  const socketRef = useRef(null);
  const [selectedCoin, setSelectedCoin] = useState(null); // Stockage de l'actif sélectionné

  console.log("availableBalance:", availableBalance); // Ajoutez ce log pour vérifier availableBalance
  console.log("portfolioId:", portfolioId); // Ajoutez ce log pour vérifier portfolioId
  console.log("token:", token); // Ajoutez ce log pour vérifier token


  const calculateChange = (prices, hours) => {
    if (!prices || prices.length === 0) return 0;
    
    const latestPrice = prices[prices.length - 1]?.price || 0;
    const pastTimestamp = Date.now() - hours * 60 * 60 * 1000;

    // Trouver le prix passé le plus proche
    const pastPriceObj = prices.reduce((closest, current) => 
        Math.abs(current.timestamp - pastTimestamp) < Math.abs(closest.timestamp - pastTimestamp) ? current : closest, 
        prices[0] // Initialisation avec le premier prix
    );

    const pastPrice = pastPriceObj ? pastPriceObj.price : latestPrice;

    // Calcul du changement en pourcentage
    return pastPrice !== 0 ? ((latestPrice - pastPrice) / pastPrice) * 100 : 0;
  };

  useEffect(() => {
    // Connexion WebSocket
    const connectWebSocket = () => {
      const socket = new WebSocket("ws://localhost:8080/market-data");

      socket.onopen = () => {
        console.log("WebSocket connecté");
      };

      socket.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            console.log("Données reçues :", data);
            console.log("Données de la catégorie actuelle :", data[category]); // Debug
    
            // Vérification si les données existent bien
            if (data[category] && Object.keys(data[category]).length > 0) {
                setAssets(Object.values(data[category]));
            } else {
                console.warn("Aucune donnée trouvée pour la catégorie :", category);
                setAssets([]); // Réinitialiser pour éviter l'affichage de données invalides
            }
        } catch (error) {
            console.error("Erreur de parsing JSON :", error);
        }
      };
    
      socket.onerror = (err) => {
        console.error("Erreur WebSocket :", err);
        socket.close();
      };

      socket.onclose = (e) => {
        console.log("WebSocket fermé. Nouvelle tentative dans 3 secondes.", e.reason);
        setTimeout(connectWebSocket, 3000);
      };

      socketRef.current = socket;
    };

    connectWebSocket();

    return () => {
      if (socketRef.current) {
        socketRef.current.close();
      }
    };
  }, [category]); // Mise à jour des données lorsqu'on change de catégorie

  // Filtrage des actifs selon la recherche
  const filteredAssets = assets.filter((asset) => {
    const symbol = asset.symbol ? asset.symbol.toLowerCase() : "";
    const name = asset.longName ? asset.longName.toLowerCase() : "";
    return symbol.includes(searchTerm.toLowerCase()) || name.includes(searchTerm.toLowerCase());
  });
  
  return (
    <div style={{ padding: "20px", backgroundColor: "#121212", color: "white" }}>
      {/* Barre de recherche + Sélecteur de catégorie */}
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "10px" }}>
        <TextField
          variant="outlined"
          placeholder="Search here..."
          onChange={(e) => setSearchTerm(e.target.value)}
          InputProps={{ style: { color: "white" } }}
          sx={{ backgroundColor: "#1e1e1e", width: "40%" }}
        />
        <Select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          sx={{ backgroundColor: "#1e1e1e", color: "white" }}
        >
          <MenuItem value="crypto">Cryptomonnaies</MenuItem>
          <MenuItem value="stocks">Actions</MenuItem>
          <MenuItem value="forex">Devises</MenuItem>
        </Select>
      </div>

      {/* Tableau des actifs */}
      <TableContainer component={Paper} style={{ backgroundColor: "black", color: "white" }}>
        <Table>
          <TableHead>
            <TableRow style={{ backgroundColor: "#2a2a2a" }}>
              <TableCell align="center" style={{ color: "darkgrey" }}><b>Asset</b></TableCell>
              <TableCell align="center" style={{ color: "darkgrey" }}><b>Name</b></TableCell>
              <TableCell align="center" style={{ color: "darkgrey" }}><b>Price</b></TableCell>
              <TableCell align="center" style={{ color: "darkgrey" }}><b>Market Cap Change</b></TableCell>
              <TableCell align="center" style={{ color: "darkgrey" }}><b>1H</b></TableCell>
              <TableCell align="center" style={{ color: "darkgrey" }}><b>24H</b></TableCell>
              <TableCell align="center" style={{ color: "darkgrey" }}><b>7D</b></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredAssets.map((asset) => (
              <TableRow key={asset.symbol} style={{ cursor: "pointer" }} onClick={() => setSelectedCoin(asset)}>
                <TableCell align="center" style={{ color: grey[300] }}>{asset.symbol}</TableCell>
                <TableCell align="center" style={{ color: grey[300] }}>{asset.longName}</TableCell>
                <TableCell align="center" style={{ color: grey[300] }}>${asset.prices ? asset.prices[asset.prices.length - 1].price.toFixed(2) : "N/A"}</TableCell>
                <TableCell align="center" style={{ color: asset.change < 0 ? "orangered" : "springgreen" }}>
                    {asset.change ? asset.change.toFixed(3) + "%" : "N/A"}
                </TableCell>
                <TableCell align="center" style={{ color: calculateChange(asset.prices, 1) < 0 ? "orangered" : "springgreen" }}>
                    {calculateChange(asset.prices, 1).toFixed(3) + "%"}
                </TableCell>
                <TableCell align="center" style={{ color: calculateChange(asset.prices, 24) < 0 ? "orangered" : "springgreen" }}>
                    {calculateChange(asset.prices, 24).toFixed(3) + "%"}
                </TableCell>
                <TableCell align="center" style={{ color: calculateChange(asset.prices, 168) < 0 ? "orangered" : "springgreen" }}>
                    {calculateChange(asset.prices, 168).toFixed(3) + "%"}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={Boolean(selectedCoin)} onClose={() => setSelectedCoin(null)} fullWidth maxWidth="md">
      {selectedCoin && (
        <CoinInfo
          coin={selectedCoin}
          portfolioId={portfolioId}
          token={token}
          walletBalance={walletBalance}
          availableBalance={walletBalance}
          onAssetAdded={onAssetAdded}
        />
      )}
    </Dialog>
    </div>
  );
};

export default CoinsTable;