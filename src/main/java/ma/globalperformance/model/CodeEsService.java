package ma.globalperformance.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ma.globalperformance.repository.ClientTransactionRepository;

@Service
public class CodeEsService {

    @Autowired
    private ClientTransactionRepository clientTransactionRepository; // Remplacez par votre repository

    public List<String> getCodeEsList() {
        // Implémentation pour récupérer la liste des codes ES depuis la base de données
        return clientTransactionRepository.findUniqueCodeEs();
    }
}

