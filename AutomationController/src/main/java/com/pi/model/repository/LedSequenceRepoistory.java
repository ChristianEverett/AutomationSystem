package com.pi.model.repository;

import org.springframework.stereotype.Repository;

import com.pi.model.LedSequence;

@Repository(RepositoryType.LedSequence)
public class LedSequenceRepoistory extends BaseRepository<String, LedSequence>
{

}
