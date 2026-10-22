-- R__seed_dev.sql
-- Données de démonstration, chargées UNIQUEMENT avec le profil "dev" (dossier db/seed).
-- 15 clients, 50 dossiers avec statuts variés, notes, pièces et événements cohérents.
-- Le script ne fait rien si la base contient déjà des dossiers (il peut donc être rejoué sans risque).
--
-- Comptes de test (mot de passe : atelier123) : zak (ADMIN), pere, frere (TECHNICIEN).

UPDATE employe SET password_hash = '{bcrypt}$2a$10$JDCsmkkE2nFPEeEd8Wa3bOQYS7TFIC.5HKYmDKIk6FGJ1X8RnUfoS'
WHERE login IN ('zak', 'pere', 'frere') AND password_hash IS NULL;
UPDATE employe SET role = 'ADMIN' WHERE login = 'zak';

DO $seed$
BEGIN
  IF EXISTS (SELECT 1 FROM dossier) THEN
    RAISE NOTICE 'Seed dev ignoré : la base contient déjà des dossiers';
    RETURN;
  END IF;

    -- ============================================
    -- CLIENTS (15 clients réalistes)
    -- ============================================
    INSERT INTO client (nom, telephone, created_at) VALUES
      ('Martin Dubois', '0612345678', NOW() - INTERVAL '30 days'),
      ('Sophie Laurent', '0623456789', NOW() - INTERVAL '28 days'),
      ('Pierre Moreau', '0634567890', NOW() - INTERVAL '25 days'),
      ('Marie Bernard', '0645678901', NOW() - INTERVAL '22 days'),
      ('Jean Petit', '0656789012', NOW() - INTERVAL '20 days'),
      ('Camille Durand', '0667890123', NOW() - INTERVAL '18 days'),
      ('Thomas Leroy', '0678901234', NOW() - INTERVAL '15 days'),
      ('Emma Rousseau', '0689012345', NOW() - INTERVAL '12 days'),
      ('Lucas Vincent', '0690123456', NOW() - INTERVAL '10 days'),
      ('Léa Martin', '0601234567', NOW() - INTERVAL '8 days'),
      ('Hugo Girard', '0613456789', NOW() - INTERVAL '6 days'),
      ('Chloé Lefebvre', '0624567890', NOW() - INTERVAL '5 days'),
      ('Nathan Simon', '0635678901', NOW() - INTERVAL '4 days'),
      ('Manon Michel', '0646789012', NOW() - INTERVAL '3 days'),
      ('Alexandre Garcia', '0657890123', NOW() - INTERVAL '1 day');

    -- ============================================
    -- DOSSIERS (50 dossiers avec statuts variés)
    -- ============================================
    -- Références au format PCP-YYYY-NNNNNN
    -- Statuts répartis : RECU (10), DIAGNOSTIC (8), ATTENTE_CLIENT (5), ATTENTE_PIECE (8), REPARATION (11), PRET (4), CLOTURE (4)

    -- Dossiers RECU (10)
    INSERT INTO dossier (reference, client_id, type_appareil, marque, modele, numero_serie, statut, priorite, technicien_assigne_id, date_entree, date_estimee, created_at) VALUES
      ('PCP-2026-000001', 1, 'PORTABLE', 'Lenovo', 'ThinkPad X1 Carbon', 'SN-LNV-001', 'RECU', 'NORMALE', 1, CURRENT_DATE - 2, CURRENT_DATE + 5, NOW() - INTERVAL '2 days'),
      ('PCP-2026-000002', 2, 'PORTABLE', 'Dell', 'XPS 13', 'SN-DEL-002', 'RECU', 'HAUTE', 2, CURRENT_DATE - 1, CURRENT_DATE + 3, NOW() - INTERVAL '1 day'),
      ('PCP-2026-000003', 3, 'TOUR', 'HP', 'Pavilion Gaming', 'SN-HP-003', 'RECU', 'NORMALE', NULL, CURRENT_DATE, CURRENT_DATE + 7, NOW()),
      ('PCP-2026-000004', 4, 'PORTABLE', 'ASUS', 'ROG Strix', 'SN-ASU-004', 'RECU', 'BASSE', NULL, CURRENT_DATE, CURRENT_DATE + 10, NOW()),
      ('PCP-2026-000005', 5, 'AUTRE', 'Apple', 'MacBook Pro', 'SN-APP-005', 'RECU', 'NORMALE', 1, CURRENT_DATE - 1, CURRENT_DATE + 5, NOW() - INTERVAL '1 day'),
      ('PCP-2026-000006', 6, 'PORTABLE', 'Acer', 'Predator', 'SN-ACE-006', 'RECU', 'HAUTE', 2, CURRENT_DATE, CURRENT_DATE + 4, NOW()),
      ('PCP-2026-000007', 7, 'TOUR', 'MSI', 'MAG Infinite', 'SN-MSI-007', 'RECU', 'NORMALE', NULL, CURRENT_DATE - 1, CURRENT_DATE + 6, NOW() - INTERVAL '1 day'),
      ('PCP-2026-000008', 8, 'PORTABLE', 'Lenovo', 'IdeaPad', 'SN-LNV-008', 'RECU', 'BASSE', NULL, CURRENT_DATE, CURRENT_DATE + 8, NOW()),
      ('PCP-2026-000009', 9, 'PORTABLE', 'Dell', 'Inspiron', 'SN-DEL-009', 'RECU', 'NORMALE', 1, CURRENT_DATE - 2, CURRENT_DATE + 5, NOW() - INTERVAL '2 days'),
      ('PCP-2026-000010', 10, 'AUTRE', 'Samsung', 'Galaxy Book', 'SN-SAM-010', 'RECU', 'HAUTE', 2, CURRENT_DATE, CURRENT_DATE + 3, NOW());

    -- Dossiers DIAGNOSTIC (8)
    INSERT INTO dossier (reference, client_id, type_appareil, marque, modele, numero_serie, statut, priorite, technicien_assigne_id, date_entree, date_estimee, created_at) VALUES
      ('PCP-2026-000011', 11, 'PORTABLE', 'HP', 'EliteBook', 'SN-HP-011', 'DIAGNOSTIC', 'NORMALE', 1, CURRENT_DATE - 5, CURRENT_DATE + 2, NOW() - INTERVAL '5 days'),
      ('PCP-2026-000012', 12, 'TOUR', 'Corsair', 'One', 'SN-COR-012', 'DIAGNOSTIC', 'HAUTE', 2, CURRENT_DATE - 4, CURRENT_DATE + 1, NOW() - INTERVAL '4 days'),
      ('PCP-2026-000013', 13, 'PORTABLE', 'ASUS', 'ZenBook', 'SN-ASU-013', 'DIAGNOSTIC', 'NORMALE', 1, CURRENT_DATE - 6, CURRENT_DATE + 3, NOW() - INTERVAL '6 days'),
      ('PCP-2026-000014', 14, 'PORTABLE', 'Lenovo', 'Legion', 'SN-LNV-014', 'DIAGNOSTIC', 'BASSE', 2, CURRENT_DATE - 5, CURRENT_DATE + 4, NOW() - INTERVAL '5 days'),
      ('PCP-2026-000015', 15, 'TOUR', 'Alienware', 'Aurora', 'SN-ALI-015', 'DIAGNOSTIC', 'HAUTE', 1, CURRENT_DATE - 4, CURRENT_DATE + 2, NOW() - INTERVAL '4 days'),
      ('PCP-2026-000016', 1, 'PORTABLE', 'Dell', 'Latitude', 'SN-DEL-016', 'DIAGNOSTIC', 'NORMALE', 2, CURRENT_DATE - 7, CURRENT_DATE + 2, NOW() - INTERVAL '7 days'),
      ('PCP-2026-000017', 2, 'AUTRE', 'Apple', 'iMac', 'SN-APP-017', 'DIAGNOSTIC', 'NORMALE', 1, CURRENT_DATE - 6, CURRENT_DATE + 3, NOW() - INTERVAL '6 days'),
      ('PCP-2026-000018', 3, 'PORTABLE', 'Acer', 'Swift', 'SN-ACE-018', 'DIAGNOSTIC', 'BASSE', 2, CURRENT_DATE - 5, CURRENT_DATE + 5, NOW() - INTERVAL '5 days');

    -- Dossiers ATTENTE_CLIENT (5)
    INSERT INTO dossier (reference, client_id, type_appareil, marque, modele, numero_serie, statut, priorite, technicien_assigne_id, date_entree, date_estimee, created_at) VALUES
      ('PCP-2026-000019', 4, 'PORTABLE', 'MSI', 'GS66', 'SN-MSI-019', 'ATTENTE_CLIENT', 'HAUTE', 1, CURRENT_DATE - 10, CURRENT_DATE + 1, NOW() - INTERVAL '10 days'),
      ('PCP-2026-000020', 5, 'TOUR', 'HP', 'Omen', 'SN-HP-020', 'ATTENTE_CLIENT', 'NORMALE', 2, CURRENT_DATE - 9, CURRENT_DATE + 2, NOW() - INTERVAL '9 days'),
      ('PCP-2026-000021', 6, 'PORTABLE', 'Lenovo', 'ThinkPad', 'SN-LNV-021', 'ATTENTE_CLIENT', 'NORMALE', 1, CURRENT_DATE - 11, CURRENT_DATE, NOW() - INTERVAL '11 days'),
      ('PCP-2026-000022', 7, 'AUTRE', 'Dell', 'OptiPlex', 'SN-DEL-022', 'ATTENTE_CLIENT', 'BASSE', 2, CURRENT_DATE - 8, CURRENT_DATE + 3, NOW() - INTERVAL '8 days'),
      ('PCP-2026-000023', 8, 'PORTABLE', 'ASUS', 'VivoBook', 'SN-ASU-023', 'ATTENTE_CLIENT', 'HAUTE', 1, CURRENT_DATE - 10, CURRENT_DATE + 1, NOW() - INTERVAL '10 days');

    -- Dossiers ATTENTE_PIECE (8)
    INSERT INTO dossier (reference, client_id, type_appareil, marque, modele, numero_serie, statut, priorite, technicien_assigne_id, date_entree, date_estimee, created_at) VALUES
      ('PCP-2026-000024', 9, 'PORTABLE', 'HP', 'ProBook', 'SN-HP-024', 'ATTENTE_PIECE', 'NORMALE', 1, CURRENT_DATE - 12, CURRENT_DATE + 2, NOW() - INTERVAL '12 days'),
      ('PCP-2026-000025', 10, 'TOUR', 'Corsair', 'Vengeance', 'SN-COR-025', 'ATTENTE_PIECE', 'HAUTE', 2, CURRENT_DATE - 11, CURRENT_DATE + 1, NOW() - INTERVAL '11 days'),
      ('PCP-2026-000026', 11, 'PORTABLE', 'Dell', 'XPS 15', 'SN-DEL-026', 'ATTENTE_PIECE', 'NORMALE', 1, CURRENT_DATE - 13, CURRENT_DATE + 3, NOW() - INTERVAL '13 days'),
      ('PCP-2026-000027', 12, 'PORTABLE', 'Lenovo', 'Yoga', 'SN-LNV-027', 'ATTENTE_PIECE', 'BASSE', 2, CURRENT_DATE - 10, CURRENT_DATE + 4, NOW() - INTERVAL '10 days'),
      ('PCP-2026-000028', 13, 'AUTRE', 'Apple', 'Mac Mini', 'SN-APP-028', 'ATTENTE_PIECE', 'HAUTE', 1, CURRENT_DATE - 12, CURRENT_DATE + 2, NOW() - INTERVAL '12 days'),
      ('PCP-2026-000029', 14, 'PORTABLE', 'ASUS', 'TUF Gaming', 'SN-ASU-029', 'ATTENTE_PIECE', 'NORMALE', 2, CURRENT_DATE - 11, CURRENT_DATE + 3, NOW() - INTERVAL '11 days'),
      ('PCP-2026-000030', 15, 'TOUR', 'MSI', 'MEG', 'SN-MSI-030', 'ATTENTE_PIECE', 'NORMALE', 1, CURRENT_DATE - 13, CURRENT_DATE + 2, NOW() - INTERVAL '13 days'),
      ('PCP-2026-000031', 1, 'PORTABLE', 'Acer', 'Nitro', 'SN-ACE-031', 'ATTENTE_PIECE', 'BASSE', 2, CURRENT_DATE - 10, CURRENT_DATE + 5, NOW() - INTERVAL '10 days');

    -- Dossiers REPARATION (6)
    INSERT INTO dossier (reference, client_id, type_appareil, marque, modele, numero_serie, statut, priorite, technicien_assigne_id, date_entree, date_estimee, created_at) VALUES
      ('PCP-2026-000032', 2, 'PORTABLE', 'HP', 'Spectre', 'SN-HP-032', 'REPARATION', 'HAUTE', 1, CURRENT_DATE - 15, CURRENT_DATE + 1, NOW() - INTERVAL '15 days'),
      ('PCP-2026-000033', 3, 'TOUR', 'Alienware', 'Area-51', 'SN-ALI-033', 'REPARATION', 'NORMALE', 2, CURRENT_DATE - 14, CURRENT_DATE + 2, NOW() - INTERVAL '14 days'),
      ('PCP-2026-000034', 4, 'PORTABLE', 'Dell', 'Precision', 'SN-DEL-034', 'REPARATION', 'NORMALE', 1, CURRENT_DATE - 16, CURRENT_DATE + 1, NOW() - INTERVAL '16 days'),
      ('PCP-2026-000035', 5, 'AUTRE', 'Apple', 'MacBook Air', 'SN-APP-035', 'REPARATION', 'HAUTE', 2, CURRENT_DATE - 15, CURRENT_DATE + 2, NOW() - INTERVAL '15 days'),
      ('PCP-2026-000036', 6, 'PORTABLE', 'Lenovo', 'ThinkPad P1', 'SN-LNV-036', 'REPARATION', 'BASSE', 1, CURRENT_DATE - 14, CURRENT_DATE + 3, NOW() - INTERVAL '14 days'),
      ('PCP-2026-000037', 7, 'PORTABLE', 'ASUS', 'ROG Zephyrus', 'SN-ASU-037', 'REPARATION', 'NORMALE', 2, CURRENT_DATE - 13, CURRENT_DATE + 2, NOW() - INTERVAL '13 days');

    -- Dossiers REPARATION, en fin de réparation (5)
    INSERT INTO dossier (reference, client_id, type_appareil, marque, modele, numero_serie, statut, priorite, technicien_assigne_id, date_entree, date_estimee, created_at) VALUES
      ('PCP-2026-000038', 8, 'PORTABLE', 'HP', 'Envy', 'SN-HP-038', 'REPARATION', 'NORMALE', 1, CURRENT_DATE - 18, CURRENT_DATE, NOW() - INTERVAL '18 days'),
      ('PCP-2026-000039', 9, 'TOUR', 'Corsair', 'One Pro', 'SN-COR-039', 'REPARATION', 'HAUTE', 2, CURRENT_DATE - 17, CURRENT_DATE + 1, NOW() - INTERVAL '17 days'),
      ('PCP-2026-000040', 10, 'PORTABLE', 'Dell', 'Alienware', 'SN-DEL-040', 'REPARATION', 'NORMALE', 1, CURRENT_DATE - 19, CURRENT_DATE, NOW() - INTERVAL '19 days'),
      ('PCP-2026-000041', 11, 'AUTRE', 'Apple', 'Mac Pro', 'SN-APP-041', 'REPARATION', 'BASSE', 2, CURRENT_DATE - 18, CURRENT_DATE + 1, NOW() - INTERVAL '18 days'),
      ('PCP-2026-000042', 12, 'PORTABLE', 'Lenovo', 'IdeaPad Gaming', 'SN-LNV-042', 'REPARATION', 'NORMALE', 1, CURRENT_DATE - 17, CURRENT_DATE, NOW() - INTERVAL '17 days');

    -- Dossiers PRET (4)
    INSERT INTO dossier (reference, client_id, type_appareil, marque, modele, numero_serie, statut, priorite, technicien_assigne_id, date_entree, date_estimee, created_at) VALUES
      ('PCP-2026-000043', 13, 'PORTABLE', 'ASUS', 'ExpertBook', 'SN-ASU-043', 'PRET', 'NORMALE', 2, CURRENT_DATE - 20, CURRENT_DATE - 1, NOW() - INTERVAL '20 days'),
      ('PCP-2026-000044', 14, 'TOUR', 'MSI', 'Trident', 'SN-MSI-044', 'PRET', 'HAUTE', 1, CURRENT_DATE - 21, CURRENT_DATE - 2, NOW() - INTERVAL '21 days'),
      ('PCP-2026-000045', 15, 'PORTABLE', 'Acer', 'Aspire', 'SN-ACE-045', 'PRET', 'NORMALE', 2, CURRENT_DATE - 19, CURRENT_DATE - 1, NOW() - INTERVAL '19 days'),
      ('PCP-2026-000046', 1, 'AUTRE', 'Dell', 'XPS Tower', 'SN-DEL-046', 'PRET', 'BASSE', 1, CURRENT_DATE - 20, CURRENT_DATE - 2, NOW() - INTERVAL '20 days');

    -- Dossiers CLOTURE (4) - avec closed_at
    INSERT INTO dossier (reference, client_id, type_appareil, marque, modele, numero_serie, statut, priorite, technicien_assigne_id, date_entree, date_estimee, created_at, closed_at) VALUES
      ('PCP-2026-000047', 2, 'PORTABLE', 'HP', 'Pavilion', 'SN-HP-047', 'CLOTURE', 'NORMALE', 2, CURRENT_DATE - 25, CURRENT_DATE - 5, NOW() - INTERVAL '25 days', NOW() - INTERVAL '5 days'),
      ('PCP-2026-000048', 3, 'TOUR', 'Corsair', 'One i200', 'SN-COR-048', 'CLOTURE', 'HAUTE', 1, CURRENT_DATE - 24, CURRENT_DATE - 4, NOW() - INTERVAL '24 days', NOW() - INTERVAL '4 days'),
      ('PCP-2026-000049', 4, 'PORTABLE', 'Lenovo', 'Legion 5', 'SN-LNV-049', 'CLOTURE', 'NORMALE', 2, CURRENT_DATE - 23, CURRENT_DATE - 3, NOW() - INTERVAL '23 days', NOW() - INTERVAL '3 days'),
      ('PCP-2026-000050', 5, 'AUTRE', 'Apple', 'Mac Studio', 'SN-APP-050', 'CLOTURE', 'BASSE', 1, CURRENT_DATE - 22, CURRENT_DATE - 2, NOW() - INTERVAL '22 days', NOW() - INTERVAL '2 days');

    -- Dossier bloqué (pour tester le workflow)

    -- ============================================
    -- NOTES (0-5 notes par dossier, ~80 notes au total)
    -- ============================================
    -- Notes pour quelques dossiers variés
    INSERT INTO note (dossier_id, contenu, sensible, created_at) VALUES
      -- Dossier 1 (RECU)
      (1, 'Client a signalé écran noir au démarrage. Vérification batterie nécessaire.', FALSE, NOW() - INTERVAL '2 days' + INTERVAL '2 hours'),
      (1, 'Batterie testée : OK. Problème probablement lié à la carte mère.', FALSE, NOW() - INTERVAL '2 days' + INTERVAL '4 hours'),

      -- Dossier 11 (DIAGNOSTIC)
      (11, 'Diagnostic initial : problème de surchauffe. Ventilateur encrassé.', FALSE, NOW() - INTERVAL '5 days' + INTERVAL '1 hour'),
      (11, 'Nettoyage effectué. Tests en cours.', FALSE, NOW() - INTERVAL '5 days' + INTERVAL '3 hours'),

      -- Dossier 19 (ATTENTE_CLIENT)
      (19, 'Devis envoyé au client : 250€ pour remplacement écran.', FALSE, NOW() - INTERVAL '10 days' + INTERVAL '2 hours'),
      (19, 'Rappel client effectué. En attente de réponse.', FALSE, NOW() - INTERVAL '8 days' + INTERVAL '10 hours'),
      (19, 'NOTE SENSIBLE: Client a mentionné un problème de garantie. À vérifier.', TRUE, NOW() - INTERVAL '7 days' + INTERVAL '14 hours'),

      -- Dossier 24 (ATTENTE_PIECE)
      (24, 'Commande passée chez fournisseur : écran LCD 15.6"', FALSE, NOW() - INTERVAL '12 days' + INTERVAL '1 hour'),
      (24, 'Fournisseur confirme livraison sous 3 jours ouvrés.', FALSE, NOW() - INTERVAL '11 days' + INTERVAL '8 hours'),

      -- Dossier 32 (REPARATION)
      (32, 'Remplacement carte mère effectué. Tests de stabilité en cours.', FALSE, NOW() - INTERVAL '15 days' + INTERVAL '2 hours'),
      (32, 'Système stable après 24h de tests. Prêt pour tests finaux.', FALSE, NOW() - INTERVAL '14 days' + INTERVAL '6 hours'),

      -- Dossier 38 (REPARATION)
      (38, 'Tests de performance : OK. Températures normales.', FALSE, NOW() - INTERVAL '18 days' + INTERVAL '3 hours'),
      (38, 'Tests de connectivité : tous les ports fonctionnels.', FALSE, NOW() - INTERVAL '18 days' + INTERVAL '5 hours'),
      (38, 'Validation finale effectuée. Prêt pour remise au client.', FALSE, NOW() - INTERVAL '17 days' + INTERVAL '2 hours'),

      -- Dossier 43 (PRET)
      (43, 'Appareil prêt. Client informé par SMS.', FALSE, NOW() - INTERVAL '20 days' + INTERVAL '4 hours'),

      -- Dossier 47 (CLOTURE)
      (47, 'Remise effectuée au client. Satisfaction confirmée.', FALSE, NOW() - INTERVAL '25 days' + INTERVAL '2 hours'),
      (47, 'Garantie 3 mois activée. Facture émise.', FALSE, NOW() - INTERVAL '25 days' + INTERVAL '3 hours');

    -- Ajout de notes supplémentaires pour quelques dossiers
    INSERT INTO note (dossier_id, contenu, sensible, created_at)
    SELECT 
      dossier_id,
      CASE 
        WHEN random() < 0.3 THEN 'Diagnostic approfondi nécessaire. ' || 
          CASE (random() * 3)::int
            WHEN 0 THEN 'Problème matériel suspecté.'
            WHEN 1 THEN 'Problème logiciel possible.'
            ELSE 'Nécessite tests supplémentaires.'
          END
        WHEN random() < 0.6 THEN 'Client contacté. ' ||
          CASE (random() * 2)::int
            WHEN 0 THEN 'En attente de réponse.'
            ELSE 'Réponse reçue, traitement en cours.'
          END
        ELSE 'Mise à jour du statut. ' ||
          CASE (random() * 2)::int
            WHEN 0 THEN 'Progression normale.'
            ELSE 'Délai estimé respecté.'
          END
      END,
      random() < 0.1, -- 10% de notes sensibles
      NOW() - (random() * 20)::int * INTERVAL '1 day' + (random() * 8)::int * INTERVAL '1 hour'
    FROM (
      SELECT id as dossier_id FROM dossier ORDER BY random() LIMIT 30
    ) d;

    -- ============================================
    -- PIECES (0-3 pièces par dossier, ~60 pièces au total)
    -- ============================================
    -- Pièces pour dossiers en ATTENTE_PIECE, REPARATION
    INSERT INTO piece (dossier_id, reference, fournisseur, cout, etat, created_at) VALUES
      -- Dossier 24 (ATTENTE_PIECE)
      (24, 'LCD-15.6-FHD-001', 'TechParts', 89.50, 'COMMANDEE', NOW() - INTERVAL '12 days' + INTERVAL '2 hours'),
      (24, 'CABLE-LCD-001', 'TechParts', 12.00, 'COMMANDEE', NOW() - INTERVAL '12 days' + INTERVAL '2 hours'),

      -- Dossier 25 (ATTENTE_PIECE)
      (25, 'RAM-DDR4-16GB', 'MemoryStore', 65.00, 'RECUE', NOW() - INTERVAL '11 days' + INTERVAL '1 hour'),
      (25, 'SSD-NVME-512GB', 'StoragePro', 45.00, 'COMMANDEE', NOW() - INTERVAL '11 days' + INTERVAL '1 hour'),

      -- Dossier 26 (ATTENTE_PIECE)
      (26, 'ECRAN-13.3-4K', 'DisplayTech', 180.00, 'A_COMMANDER', NOW() - INTERVAL '13 days' + INTERVAL '3 hours'),

      -- Dossier 28 (ATTENTE_PIECE)
      (28, 'CLAVIER-ISO-FR', 'KeyParts', 35.00, 'RECUE', NOW() - INTERVAL '12 days' + INTERVAL '1 hour'),
      (28, 'TOUCHPAD-001', 'KeyParts', 28.00, 'MONTEE', NOW() - INTERVAL '12 days' + INTERVAL '2 hours'),

      -- Dossier 30 (ATTENTE_PIECE)
      (30, 'VENTILATEUR-CPU', 'CoolingPro', 25.00, 'RECUE', NOW() - INTERVAL '13 days' + INTERVAL '2 hours'),
      (30, 'PASTE-THERMIQUE', 'CoolingPro', 8.00, 'MONTEE', NOW() - INTERVAL '13 days' + INTERVAL '3 hours'),

      -- Dossier 32 (REPARATION)
      (32, 'CARTE-MERE-MAIN', 'MotherboardCo', 320.00, 'MONTEE', NOW() - INTERVAL '15 days' + INTERVAL '1 hour'),
      (32, 'PROCESSEUR-I7', 'CPUSupply', 450.00, 'MONTEE', NOW() - INTERVAL '15 days' + INTERVAL '2 hours'),

      -- Dossier 33 (REPARATION)
      (33, 'ALIMENTATION-750W', 'PowerSupplyInc', 95.00, 'MONTEE', NOW() - INTERVAL '14 days' + INTERVAL '1 hour'),

      -- Dossier 34 (REPARATION)
      (34, 'ECRAN-14-FHD', 'DisplayTech', 120.00, 'MONTEE', NOW() - INTERVAL '16 days' + INTERVAL '2 hours'),
      (34, 'HINGE-ECRAN-001', 'MechanicalParts', 45.00, 'MONTEE', NOW() - INTERVAL '16 days' + INTERVAL '3 hours'),

      -- Dossier 36 (REPARATION)
      (36, 'CARTE-GRAPHIQUE-RTX', 'GPUSupply', 650.00, 'MONTEE', NOW() - INTERVAL '14 days' + INTERVAL '1 hour');

    -- Ajout de pièces supplémentaires pour dossiers variés
    INSERT INTO piece (dossier_id, reference, fournisseur, cout, etat, created_at)
    SELECT 
      dossier_id,
      'PART-' || LPAD((random() * 9999)::int::text, 4, '0'),
      CASE (random() * 5)::int
        WHEN 0 THEN 'TechParts'
        WHEN 1 THEN 'MemoryStore'
        WHEN 2 THEN 'DisplayTech'
        WHEN 3 THEN 'CoolingPro'
        ELSE 'KeyParts'
      END,
      ROUND((random() * 500 + 10)::numeric, 2),
      CASE (random() * 4)::int
        WHEN 0 THEN 'A_COMMANDER'
        WHEN 1 THEN 'COMMANDEE'
        WHEN 2 THEN 'RECUE'
        ELSE 'MONTEE'
      END,
      NOW() - (random() * 20)::int * INTERVAL '1 day' + (random() * 8)::int * INTERVAL '1 hour'
    FROM (
      SELECT id as dossier_id FROM dossier 
      WHERE statut IN ('ATTENTE_PIECE', 'REPARATION', 'PRET')
      ORDER BY random() LIMIT 25
    ) d;

    -- ============================================
    -- EVENEMENTS (timeline cohérente pour chaque dossier)
    -- ============================================
    -- Événements CREATION_DOSSIER pour tous les dossiers
    INSERT INTO evenement (dossier_id, employe_id, type_action, commentaire, created_at)
    SELECT 
      id,
      CASE (random() * 3)::int + 1
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        ELSE 3
      END,
      'CREATION_DOSSIER',
      'Dossier créé',
      created_at
    FROM dossier;

    -- Événements CHANGEMENT_STATUT pour dossiers avec progression
    INSERT INTO evenement (dossier_id, employe_id, type_action, ancienne_valeur, nouvelle_valeur, commentaire, created_at) VALUES
      -- Dossier 11 (DIAGNOSTIC) : RECU -> DIAGNOSTIC
      (11, 1, 'CHANGEMENT_STATUT', 'RECU', 'DIAGNOSTIC', 'Début du diagnostic', NOW() - INTERVAL '5 days' + INTERVAL '2 hours'),

      -- Dossier 19 (ATTENTE_CLIENT) : DIAGNOSTIC -> ATTENTE_CLIENT
      (19, 1, 'CHANGEMENT_STATUT', 'DIAGNOSTIC', 'ATTENTE_CLIENT', 'Devis envoyé au client', NOW() - INTERVAL '10 days' + INTERVAL '3 hours'),

      -- Dossier 24 (ATTENTE_PIECE) : DIAGNOSTIC -> ATTENTE_PIECE
      (24, 1, 'CHANGEMENT_STATUT', 'DIAGNOSTIC', 'ATTENTE_PIECE', 'Pièces commandées', NOW() - INTERVAL '12 days' + INTERVAL '2 hours'),

      -- Dossier 32 (REPARATION) : ATTENTE_PIECE -> REPARATION
      (32, 1, 'CHANGEMENT_STATUT', 'ATTENTE_PIECE', 'REPARATION', 'Pièces reçues, début réparation', NOW() - INTERVAL '15 days' + INTERVAL '1 hour'),

      -- Dossier 43 (PRET) : REPARATION -> PRET
      (43, 2, 'CHANGEMENT_STATUT', 'REPARATION', 'PRET', 'Tests validés, prêt pour remise', NOW() - INTERVAL '20 days' + INTERVAL '4 hours'),

      -- Dossier 47 (CLOTURE) : PRET -> CLOTURE
      (47, 2, 'CHANGEMENT_STATUT', 'PRET', 'CLOTURE', 'Remis au client', NOW() - INTERVAL '25 days' + INTERVAL '2 hours'),
      (47, 2, 'CLOTURE', NULL, NULL, 'Dossier clôturé', NOW() - INTERVAL '25 days' + INTERVAL '3 hours');

    -- Événements AJOUT_NOTE pour quelques notes importantes
    INSERT INTO evenement (dossier_id, employe_id, type_action, commentaire, created_at)
    SELECT 
      n.dossier_id,
      CASE (random() * 3)::int + 1
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        ELSE 3
      END,
      'AJOUT_NOTE',
      'Note ajoutée',
      n.created_at
    FROM note n
    WHERE n.sensible = TRUE OR random() < 0.3
    LIMIT 20;

    -- Événements AJOUT_PIECE pour pièces ajoutées
    INSERT INTO evenement (dossier_id, employe_id, type_action, nouvelle_valeur, commentaire, created_at)
    SELECT 
      p.dossier_id,
      CASE (random() * 3)::int + 1
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        ELSE 3
      END,
      'AJOUT_PIECE',
      p.reference || ' (' || p.fournisseur || ')',
      'Pièce ajoutée',
      p.created_at
    FROM piece p
    WHERE random() < 0.5
    LIMIT 30;

    -- Événements MODIF_PIECE pour changements d'état de pièces
    INSERT INTO evenement (dossier_id, employe_id, type_action, ancienne_valeur, nouvelle_valeur, commentaire, created_at)
    SELECT 
      p.dossier_id,
      CASE (random() * 3)::int + 1
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        ELSE 3
      END,
      'MODIF_PIECE',
      'A_COMMANDER',
      p.etat,
      'État de la pièce modifié',
      p.created_at + INTERVAL '1 hour'
    FROM piece p
    WHERE p.etat != 'A_COMMANDER'
    LIMIT 20;

    -- Événements AJOUT_PHOTO pour quelques photos
    INSERT INTO evenement (dossier_id, employe_id, type_action, nouvelle_valeur, commentaire, created_at)
    SELECT 
      ph.dossier_id,
      CASE (random() * 3)::int + 1
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        ELSE 3
      END,
      'AJOUT_PHOTO',
      ph.chemin_fichier,
      'Photo ajoutée',
      ph.created_at
    FROM photo ph
    WHERE random() < 0.4
    LIMIT 20;

    -- Événements ACTION_SENSIBLE pour changements de technicien/priorité/blocage
    INSERT INTO evenement (dossier_id, employe_id, type_action, ancienne_valeur, nouvelle_valeur, commentaire, created_at) VALUES
      -- Changement technicien
      (1, 1, 'ACTION_SENSIBLE', 'Technicien: NULL', 'Technicien: Pere', 'Assignation technicien', NOW() - INTERVAL '2 days' + INTERVAL '3 hours'),
      (2, 2, 'ACTION_SENSIBLE', 'Technicien: NULL', 'Technicien: Frere', 'Assignation technicien', NOW() - INTERVAL '1 day' + INTERVAL '2 hours'),

      -- Changement priorité
      (2, 2, 'ACTION_SENSIBLE', 'Priorite: NORMALE', 'Priorite: HAUTE', 'Priorité augmentée', NOW() - INTERVAL '1 day' + INTERVAL '3 hours'),

      -- Blocage dossier
      (20, 1, 'ACTION_SENSIBLE', 'Bloque: FALSE', 'Bloque: TRUE', 'Dossier bloqué - attente validation client', NOW() - INTERVAL '9 days' + INTERVAL '4 hours');

    -- Ajout d'événements supplémentaires pour dossiers variés
    INSERT INTO evenement (dossier_id, employe_id, type_action, ancienne_valeur, nouvelle_valeur, commentaire, created_at)
    SELECT 
      d.id,
      CASE (random() * 3)::int + 1
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        ELSE 3
      END,
      CASE (random() * 3)::int
        WHEN 0 THEN 'CHANGEMENT_STATUT'
        WHEN 1 THEN 'AJOUT_NOTE'
        ELSE 'ACTION_SENSIBLE'
      END,
      CASE WHEN random() < 0.5 THEN 'RECU' ELSE NULL END,
      CASE WHEN random() < 0.5 THEN 'DIAGNOSTIC' ELSE NULL END,
      'Événement automatique',
      d.created_at + (random() * 5)::int * INTERVAL '1 day' + (random() * 8)::int * INTERVAL '1 hour'
    FROM dossier d
    WHERE random() < 0.3
    LIMIT 15;

    -- Description du problème (reprise du commentaire de création)
    UPDATE dossier d
    SET description_probleme = e.commentaire
    FROM evenement e
    WHERE e.dossier_id = d.id AND e.type_action = 'CREATION_DOSSIER' AND e.commentaire IS NOT NULL;

    -- Les références ont été écrites en dur : on recale le compteur
    INSERT INTO reference_compteur (annee, dernier)
    SELECT CAST(substring(reference FROM 5 FOR 4) AS INT), max(CAST(substring(reference FROM 10) AS INT))
    FROM dossier
    GROUP BY substring(reference FROM 5 FOR 4)
    ON CONFLICT (annee) DO UPDATE SET dernier = GREATEST(reference_compteur.dernier, EXCLUDED.dernier);
END
$seed$;
