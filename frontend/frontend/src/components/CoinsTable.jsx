import React, { useState, useEffect, useRef } from "react";
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, TextField, MenuItem, Select, Dialog } from "@mui/material";
import CoinInfo from "./CoinInfo";

const CoinsTable = ({ portfolioId, token, walletBalance, availableBalance, onAssetAdded }) => {
  const [category, setCategory] = useState("crypto");
  const [searchTerm, setSearchTerm] = useState("");
  const [assets, setAssets] = useState([]);
  const socketRef = useRef(null);
  const [selectedCoin, setSelectedCoin] = useState(null);

  const calculateChange = (prices, hours) => {
    if (!prices || prices.length === 0) return 0;

    const latestPrice = prices[prices.length - 1]?.price || 0;
    const pastTimestamp = Date.now() - hours * 60 * 60 * 1000;

    // Trouver le prix passé le plus proche
    const pastPriceObj = prices.reduce((closest, current) =>
            Math.abs(current.timestamp - pastTimestamp) < Math.abs(closest.timestamp - pastTimestamp) ? current : closest,
        prices[0]
    );

    const pastPrice = pastPriceObj ? pastPriceObj.price : latestPrice;

    // Calculer le changement en pourcentage
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
              placeholder="Recherche..."
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{ style: { color: "white" } }}
              sx={{ backgroundColor: "#1e1e1e", width: "40%" }}
          />
          <Select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              sx={{
                backgroundColor: "#1e1e1e",
                color: "darkgrey",
                borderRadius: "6px",
                "& .MuiSelect-select": { color: "white" },
              }}
              MenuProps={{
                PaperProps: {
                  sx: {
                    backgroundColor: "#1e1e1e",
                    boxShadow: "none",
                    border: "none",
                  }
                }
              }}
          >
            <MenuItem
                value="crypto"
                sx={{
                  backgroundColor: "#1e1e1e",
                  color: "white",
                  ":hover": { backgroundColor: "#333", color: "white" },
                  "&.Mui-selected": { backgroundColor: "#34d399", color: "black" },
                  "&.Mui-selected:hover": { backgroundColor: "#2aa378", color: "black" },
                }}
            >
              Crypto
            </MenuItem>

            <MenuItem
                value="stocks"
                sx={{
                  backgroundColor: "#1e1e1e",
                  color: "white",
                  ":hover": { backgroundColor: "#333", color: "white" },
                  "&.Mui-selected": { backgroundColor: "#34d399", color: "black" },
                  "&.Mui-selected:hover": { backgroundColor: "#2aa378", color: "black" },
                }}
            >
              Actions
            </MenuItem>

            <MenuItem
                value="forex"
                sx={{
                  backgroundColor: "#1e1e1e",
                  color: "white",
                  ":hover": { backgroundColor: "#333", color: "#white" },
                  "&.Mui-selected": { backgroundColor: "#34d399", color: "black" },
                  "&.Mui-selected:hover": { backgroundColor: "#2aa378", color: "black" },
                }}
            >
              Devises
            </MenuItem>
          </Select>


        </div>

        {/* Tableau des actifs */}
        <TableContainer component={Paper} style={{ backgroundColor: "#1e1e1e", color: "white" }}>
          <Table>
            <TableHead>
              <TableRow style={{ backgroundColor: "#2a2a2a" }}>
                <TableCell align="center" style={{ backgroundColor: "#3A3A3A", color: "white" }}><b>Actif</b></TableCell>
                <TableCell align="center" style={{ backgroundColor: "#3A3A3A", color: "white" }}><b>Nom</b></TableCell>
                <TableCell align="center" style={{ backgroundColor: "#3A3A3A", color: "white" }}><b>Prix</b></TableCell>
                <TableCell align="center" style={{ backgroundColor: "#3A3A3A", color: "white" }}><b>Market Cap Change</b></TableCell>
                <TableCell align="center" style={{ backgroundColor: "#3A3A3A", color: "white" }}><b>1H</b></TableCell>
                <TableCell align="center" style={{ backgroundColor: "#3A3A3A", color: "white" }}><b>24H</b></TableCell>
                <TableCell align="center" style={{ backgroundColor: "#3A3A3A", color: "white" }}><b>2J</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredAssets.map((asset) => (
                  <TableRow key={asset.symbol} style={{ cursor: "pointer" }} onClick={() => setSelectedCoin(asset)}>
                    <TableCell align="center" style={{color: "white" }}>{asset.symbol}</TableCell>
                    <TableCell align="center" style={{color: "white" }}>{asset.longName}</TableCell>
                    <TableCell align="center" style={{color: "white" }}>${asset.prices ? asset.prices[asset.prices.length - 1].price.toFixed(2) : "N/A"}</TableCell>
                    <TableCell align="center" style={{ color: asset.change < 0 ? "red" : "lightgreen" }}>
                      {asset.change ? asset.change.toFixed(3) + "%" : "N/A"}
                    </TableCell>
                    <TableCell align="center" style={{ color: calculateChange(asset.prices, 1) < 0 ? "red" : "lightgreen" }}>
                      {calculateChange(asset.prices, 1).toFixed(3) + "%"}
                    </TableCell>
                    <TableCell align="center" style={{ color: calculateChange(asset.prices, 24) < 0 ? "red" : "lightgreen" }}>
                      {calculateChange(asset.prices, 24).toFixed(3) + "%"}
                    </TableCell>
                    <TableCell align="center" style={{ color: calculateChange(asset.prices, 168) < 0 ? "red" : "lightgreen" }}>
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