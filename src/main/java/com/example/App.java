package com.example;

import com.example.model.joined.Developpeur;
import com.example.model.joined.Employe;
import com.example.model.joined.Manager;
import com.example.model.singletable.Moto;
import com.example.model.singletable.Vehicule;
import com.example.model.singletable.Voiture;
import com.example.model.tableperclass.Electronique;
import com.example.model.tableperclass.Livre;
import com.example.model.tableperclass.Produit;
import com.example.service.CrudService;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory(
                        "hibernate-inheritance"
                );

        try {
            CrudService<Vehicule> vehicules =
                    new CrudService<>(emf, Vehicule.class);
            CrudService<Employe> employes =
                    new CrudService<>(emf, Employe.class);
            CrudService<Produit> produits =
                    new CrudService<>(emf, Produit.class);

            // CREATE : SINGLE_TABLE
            Voiture voiture = new Voiture("Toyota", 4);
            Moto moto = new Moto("Harley", true);
            vehicules.creer(voiture);
            vehicules.creer(moto);

            // CREATE : JOINED
            Developpeur dev =
                    new Developpeur("Alice", "Java");
            Manager manager =
                    new Manager("Bob", 3);
            employes.creer(dev);
            employes.creer(manager);

            // CREATE : TABLE_PER_CLASS
            Livre livre =
                    new Livre("Clean Code", "Robert C. Martin");
            Electronique electronique =
                    new Electronique("Smartphone", 24);
            produits.creer(livre);
            produits.creer(electronique);

            // READ : requêtes polymorphiques
            System.out.println("--- Véhicules ---");
            vehicules.lister().forEach(
                    v -> System.out.println(v.decrire())
            );

            System.out.println("--- Employés ---");
            employes.lister().forEach(
                    e -> System.out.println(e.decrire())
            );

            System.out.println("--- Produits ---");
            produits.lister().forEach(
                    p -> System.out.println(p.decrire())
            );

            // CRITERIA : marque ou nom + sous-classe
            System.out.println(
                    "Motos Harley : "
                            + vehicules.rechercher(
                            "marque", "Har", Moto.class
                    ).size()
            );

            System.out.println(
                    "Développeurs Alice : "
                            + employes.rechercher(
                            "nom", "Ali", Developpeur.class
                    ).size()
            );

            System.out.println(
                    "Livres Clean Code : "
                            + produits.rechercher(
                            "nom", "Clean", Livre.class
                    ).size()
            );

            // UPDATE
            vehicules.modifier(
                    voiture.getId(),
                    v -> v.setMarque("Toyota modifiée")
            );

            System.out.println(
                    "Voiture modifiée : "
                            + vehicules.trouver(voiture.getId())
                            .decrire()
            );

            // DELETE
            produits.supprimer(electronique.getId());

            System.out.println(
                    "Produits après suppression : "
                            + produits.lister().size()
            );

        } finally {
            emf.close();
        }
    }
}